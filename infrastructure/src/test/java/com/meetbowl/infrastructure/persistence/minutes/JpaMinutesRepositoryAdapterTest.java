package com.meetbowl.infrastructure.persistence.minutes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesStatus;
import com.meetbowl.infrastructure.config.InfrastructureConfig;

/** MinutesRepositoryPort의 JPA 구현이 도메인 변환과 meetingId unique 제약을 지키는지 검증한다. */
@SpringBootTest(classes = JpaMinutesRepositoryAdapterTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:minutes-jpa-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class JpaMinutesRepositoryAdapterTest {

    @Autowired private JpaMinutesRepositoryAdapter adapter;

    @Test
    void saveAndFindByMeetingId() {
        UUID meetingId = UUID.randomUUID();
        Minutes minutes = minutes(meetingId);

        Minutes saved = adapter.save(minutes);

        assertThat(saved.id()).isNotNull();
        assertThat(adapter.existsByMeetingId(meetingId)).isTrue();

        Minutes found = adapter.findByMeetingId(meetingId).orElseThrow();

        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.meetingId()).isEqualTo(meetingId);
        assertThat(found.status()).isEqualTo(MinutesStatus.DRAFT);
        assertThat(found.content()).isEqualTo("회의록 본문");
    }

    @Test
    void saveApprovedMinutes() {
        UUID reviewerUserId = UUID.randomUUID();
        Instant approvedAt = Instant.parse("2099-01-01T01:00:00Z");
        Minutes approved =
                minutes(UUID.randomUUID(), reviewerUserId).approve(reviewerUserId, approvedAt);

        Minutes saved = adapter.save(approved);
        Minutes found = adapter.findById(saved.id()).orElseThrow();

        assertThat(found.status()).isEqualTo(MinutesStatus.APPROVED);
        assertThat(found.reviewerUserId()).isEqualTo(reviewerUserId);
        assertThat(found.approvedAt()).isEqualTo(approvedAt);
    }

    @Test
    void meetingIdMustBeUnique() {
        UUID meetingId = UUID.randomUUID();
        adapter.save(minutes(meetingId));

        assertThrows(DataIntegrityViolationException.class, () -> adapter.save(minutes(meetingId)));
    }

    private Minutes minutes(UUID meetingId) {
        return minutes(meetingId, UUID.randomUUID());
    }

    private Minutes minutes(UUID meetingId, UUID reviewerUserId) {
        return Minutes.createDraft(
                meetingId,
                UUID.randomUUID(),
                reviewerUserId,
                "회의 요약",
                "회의록 본문",
                "llm-model-name",
                "minutes-v1");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({InfrastructureConfig.class, JpaMinutesRepositoryAdapter.class, MinutesJpaConfig.class})
    static class TestApplication {}
}
