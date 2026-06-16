package com.meetbowl.infrastructure.persistence.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.meetbowl.domain.notification.Notification;
import com.meetbowl.domain.notification.NotificationResourceType;
import com.meetbowl.domain.notification.NotificationType;
import com.meetbowl.infrastructure.config.InfrastructureConfig;

@SpringBootTest(classes = JpaNotificationRepositoryAdapterTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:notification-jpa-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class JpaNotificationRepositoryAdapterTest {

    @Autowired private JpaNotificationRepositoryAdapter repository;

    @Test
    void savesAssignsIdAndCreatedAt() {
        Notification saved = repository.save(reminder(UUID.randomUUID(), "회의 시작 10분 전"));

        // 새 알림은 저장 시 id와 생성 시각이 채워진다.
        assertThat(saved.id()).isNotNull();
        assertThat(saved.createdAt()).isNotNull();
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    void listsRecipientNotificationsLatestFirstWithPaging() {
        UUID recipient = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        repository.save(reminder(recipient, "첫 번째"));
        repository.save(reminder(recipient, "두 번째"));
        repository.save(reminder(recipient, "세 번째"));
        repository.save(reminder(other, "남의 알림"));

        // 최신순(생성 역순)으로 한 페이지(2건)만 가져온다 — 마지막에 저장한 "세 번째"가 맨 앞.
        List<Notification> firstPage = repository.findPageByRecipientUserId(recipient, 0, 2);
        assertThat(firstPage).hasSize(2);
        assertThat(firstPage.get(0).title()).isEqualTo("세 번째");
        assertThat(firstPage.get(1).title()).isEqualTo("두 번째");

        List<Notification> secondPage = repository.findPageByRecipientUserId(recipient, 2, 2);
        assertThat(secondPage).hasSize(1);
        assertThat(secondPage.get(0).title()).isEqualTo("첫 번째");

        // 카운트는 수신자 본인 것만 센다(남의 알림 제외).
        assertThat(repository.countByRecipientUserId(recipient)).isEqualTo(3);
    }

    @Test
    void countsUnreadAndPreservesCreatedAtOnReadUpdate() {
        UUID recipient = UUID.randomUUID();
        Notification a = repository.save(reminder(recipient, "안 읽음 A"));
        repository.save(reminder(recipient, "안 읽음 B"));

        assertThat(repository.countUnreadByRecipientUserId(recipient)).isEqualTo(2);

        // 읽음 처리 후 재저장해도 생성 시각은 보존되어야 한다(createdAt은 updatable=false).
        Instant createdAt = a.createdAt();
        a.markRead(Instant.parse("2099-01-01T00:00:00Z"));
        Notification updated = repository.save(a);

        assertThat(updated.isRead()).isTrue();
        assertThat(updated.createdAt()).isEqualTo(createdAt);
        assertThat(repository.countUnreadByRecipientUserId(recipient)).isEqualTo(1);
        assertThat(repository.findUnreadByRecipientUserId(recipient)).hasSize(1);
    }

    @Test
    void findsAllNotificationsOfType() {
        // 같은 컨텍스트의 다른 테스트가 남긴 행이 있을 수 있어, 종류 일치와 내가 만든 행 포함 여부로 검증한다.
        Notification a = repository.save(reviewRequest(UUID.randomUUID(), UUID.randomUUID()));
        Notification b = repository.save(reviewRequest(UUID.randomUUID(), UUID.randomUUID()));
        // 다른 종류는 섞여 있어도 종류 조회 결과에 포함되지 않는다.
        Notification reminder = repository.save(reminder(UUID.randomUUID(), "회의 알림"));

        List<Notification> requests =
                repository.findByType(NotificationType.MINUTES_REVIEW_REQUEST);

        assertThat(requests)
                .allMatch(n -> n.type() == NotificationType.MINUTES_REVIEW_REQUEST)
                .extracting(Notification::id)
                .contains(a.id(), b.id())
                .doesNotContain(reminder.id());
    }

    @Test
    void findsLatestByRecipientTypeAndResource() {
        UUID recipient = UUID.randomUUID();
        UUID minutesId = UUID.randomUUID();
        Notification request = repository.save(reviewRequest(recipient, minutesId));
        // 같은 종류라도 다른 수신자/리소스, 다른 종류는 매칭되지 않는다.
        repository.save(reviewRequest(UUID.randomUUID(), minutesId));
        repository.save(reviewRequest(recipient, UUID.randomUUID()));
        repository.save(reminder(recipient, "회의 알림"));

        assertThat(
                        repository.findLatestByRecipientUserIdAndTypeAndResourceId(
                                recipient, NotificationType.MINUTES_REVIEW_REQUEST, minutesId))
                .get()
                .extracting(Notification::id)
                .isEqualTo(request.id());

        // 매칭되는 알림이 없으면 비어 있다.
        assertThat(
                        repository.findLatestByRecipientUserIdAndTypeAndResourceId(
                                recipient,
                                NotificationType.MINUTES_REVIEW_REMINDER,
                                minutesId))
                .isEmpty();
    }

    private Notification reminder(UUID recipient, String title) {
        return Notification.create(
                recipient,
                NotificationType.MEETING_REMINDER,
                title,
                "회의가 곧 시작됩니다.",
                NotificationResourceType.MEETING,
                UUID.randomUUID());
    }

    private Notification reviewRequest(UUID recipient, UUID minutesId) {
        return Notification.create(
                recipient,
                NotificationType.MINUTES_REVIEW_REQUEST,
                "회의록 검토 요청",
                "검토를 요청합니다.",
                NotificationResourceType.MEETING_MINUTES,
                minutesId);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        JpaNotificationRepositoryAdapter.class,
        NotificationJpaConfig.class
    })
    static class TestApplication {}
}
