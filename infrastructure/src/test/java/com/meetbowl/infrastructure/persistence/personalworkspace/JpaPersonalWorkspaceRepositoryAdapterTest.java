package com.meetbowl.infrastructure.persistence.personalworkspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEvent;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarSubscription;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFile;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemo;
import com.meetbowl.infrastructure.config.InfrastructureConfig;

@SpringBootTest(classes = JpaPersonalWorkspaceRepositoryAdapterTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:personal-workspace-jpa-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class JpaPersonalWorkspaceRepositoryAdapterTest {

    @Autowired private JpaPersonalWorkspaceCalendarEventRepositoryAdapter calendarEventAdapter;

    @Autowired
    private JpaPersonalWorkspaceCalendarSubscriptionRepositoryAdapter calendarSubscriptionAdapter;

    @Autowired private JpaPersonalWorkspaceDriveFileRepositoryAdapter driveFileAdapter;
    @Autowired private JpaPersonalWorkspaceMemoRepositoryAdapter memoAdapter;

    @Test
    void saveAndFindCalendarEventsByPeriod() {
        UUID ownerUserId = UUID.randomUUID();
        PersonalWorkspaceCalendarEvent event =
                PersonalWorkspaceCalendarEvent.createPersonal(
                        ownerUserId,
                        "개인 일정",
                        null,
                        Instant.parse("2099-01-01T01:00:00Z"),
                        Instant.parse("2099-01-01T02:00:00Z"),
                        false);

        PersonalWorkspaceCalendarEvent saved = calendarEventAdapter.save(event);

        List<PersonalWorkspaceCalendarEvent> events =
                calendarEventAdapter.findByOwnerUserIdAndPeriod(
                        ownerUserId,
                        Instant.parse("2099-01-01T00:30:00Z"),
                        Instant.parse("2099-01-01T01:30:00Z"));

        assertThat(saved.id()).isNotNull();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().title()).isEqualTo("개인 일정");
    }

    @Test
    void calendarEventLookupAndDeleteRequireOwnerUserId() {
        UUID ownerUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        PersonalWorkspaceCalendarEvent saved =
                calendarEventAdapter.save(
                        PersonalWorkspaceCalendarEvent.createPersonal(
                                ownerUserId,
                                "소유자 일정",
                                null,
                                Instant.parse("2099-01-01T01:00:00Z"),
                                Instant.parse("2099-01-01T02:00:00Z"),
                                false));

        assertThat(calendarEventAdapter.findByIdAndOwnerUserId(saved.id(), otherUserId)).isEmpty();
        assertThat(calendarEventAdapter.deletePersonalByIdAndOwnerUserId(saved.id(), otherUserId))
                .isFalse();
        assertThat(calendarEventAdapter.findByIdAndOwnerUserId(saved.id(), ownerUserId))
                .isPresent();
        assertThat(calendarEventAdapter.deletePersonalByIdAndOwnerUserId(saved.id(), ownerUserId))
                .isTrue();
    }

    @Test
    void subscribedCalendarReadsCurrentTargetEvents() {
        UUID ownerUserId = UUID.randomUUID();
        UUID subscriberUserId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2099-01-01T01:00:00Z");
        Instant endedAt = Instant.parse("2099-01-01T02:00:00Z");
        PersonalWorkspaceCalendarEvent saved =
                calendarEventAdapter.save(
                        PersonalWorkspaceCalendarEvent.createPersonal(
                                ownerUserId, "변경 전", null, startedAt, endedAt, false));
        calendarSubscriptionAdapter.save(
                PersonalWorkspaceCalendarSubscription.create(
                        subscriberUserId, ownerUserId, Instant.parse("2099-01-01T00:00:00Z")));

        calendarEventAdapter.save(saved.updatePersonal("변경 후", null, startedAt, endedAt, false));

        List<PersonalWorkspaceCalendarEvent> visibleEvents =
                calendarEventAdapter.findVisibleByUserIdAndPeriod(
                        subscriberUserId,
                        Instant.parse("2099-01-01T00:30:00Z"),
                        Instant.parse("2099-01-01T02:30:00Z"));
        assertThat(visibleEvents)
                .extracting(PersonalWorkspaceCalendarEvent::title)
                .containsExactly("변경 후");

        calendarEventAdapter.deletePersonalByIdAndOwnerUserId(saved.id(), ownerUserId);
        assertThat(
                        calendarEventAdapter.findVisibleByUserIdAndPeriod(
                                subscriberUserId,
                                Instant.parse("2099-01-01T00:30:00Z"),
                                Instant.parse("2099-01-01T02:30:00Z")))
                .isEmpty();
    }

    @Test
    void findOnlyActiveDriveFiles() {
        UUID ownerUserId = UUID.randomUUID();
        PersonalWorkspaceDriveFile activeFile =
                PersonalWorkspaceDriveFile.create(
                        ownerUserId,
                        "active.pdf",
                        "application/pdf",
                        1024L,
                        "personal-workspace/user/active.pdf",
                        Instant.parse("2099-01-01T01:00:00Z"));
        PersonalWorkspaceDriveFile deletedFile =
                PersonalWorkspaceDriveFile.create(
                                ownerUserId,
                                "deleted.pdf",
                                "application/pdf",
                                2048L,
                                "personal-workspace/user/deleted.pdf",
                                Instant.parse("2099-01-01T02:00:00Z"))
                        .delete(Instant.parse("2099-01-02T01:00:00Z"));

        driveFileAdapter.save(activeFile);
        driveFileAdapter.save(deletedFile);

        List<PersonalWorkspaceDriveFile> files =
                driveFileAdapter.findActiveByOwnerUserId(ownerUserId);

        assertThat(files).hasSize(1);
        assertThat(files.getFirst().originalFileName()).isEqualTo("active.pdf");
    }

    @Test
    void saveMemo() {
        UUID ownerUserId = UUID.randomUUID();
        PersonalWorkspaceMemo memo =
                PersonalWorkspaceMemo.create(
                        ownerUserId, "메모", "내용", Instant.parse("2099-01-01T01:00:00Z"));

        PersonalWorkspaceMemo savedMemo = memoAdapter.save(memo);

        assertThat(savedMemo.id()).isNotNull();
    }

    @Test
    void memoLookupAndDeleteRequireOwnerUserId() {
        UUID ownerUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        PersonalWorkspaceMemo saved =
                memoAdapter.save(
                        PersonalWorkspaceMemo.create(
                                ownerUserId, "메모", "내용", Instant.parse("2099-01-01T01:00:00Z")));

        assertThat(memoAdapter.findByIdAndOwnerUserId(saved.id(), otherUserId)).isEmpty();
        assertThat(memoAdapter.deleteByIdAndOwnerUserId(saved.id(), otherUserId)).isFalse();
        assertThat(memoAdapter.findByIdAndOwnerUserId(saved.id(), ownerUserId)).isPresent();
    }

    @Test
    void meetingCalendarEventIsUniquePerOwnerAndMeeting() {
        UUID ownerUserId = UUID.randomUUID();
        UUID meetingId = UUID.randomUUID();
        PersonalWorkspaceCalendarEvent event =
                PersonalWorkspaceCalendarEvent.createFromMeeting(
                        ownerUserId,
                        meetingId,
                        "회의",
                        null,
                        Instant.parse("2099-01-01T01:00:00Z"),
                        Instant.parse("2099-01-01T02:00:00Z"));

        calendarEventAdapter.save(event);

        assertThatThrownBy(() -> calendarEventAdapter.save(event))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void personalDeleteDoesNotRemoveMeetingCalendarEvent() {
        UUID meetingId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        PersonalWorkspaceCalendarEvent saved =
                calendarEventAdapter.save(
                        PersonalWorkspaceCalendarEvent.createFromMeeting(
                                ownerUserId,
                                meetingId,
                                "회의",
                                null,
                                Instant.parse("2099-01-01T01:00:00Z"),
                                Instant.parse("2099-01-01T02:00:00Z")));

        assertThat(calendarEventAdapter.deletePersonalByIdAndOwnerUserId(saved.id(), ownerUserId))
                .isFalse();
        assertThat(calendarEventAdapter.findByIdAndOwnerUserId(saved.id(), ownerUserId))
                .isPresent();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        PersonalWorkspaceJpaConfig.class,
        JpaPersonalWorkspaceCalendarEventRepositoryAdapter.class,
        JpaPersonalWorkspaceCalendarSubscriptionRepositoryAdapter.class,
        JpaPersonalWorkspaceDriveFileRepositoryAdapter.class,
        JpaPersonalWorkspaceMemoRepositoryAdapter.class
    })
    static class TestApplication {}
}
