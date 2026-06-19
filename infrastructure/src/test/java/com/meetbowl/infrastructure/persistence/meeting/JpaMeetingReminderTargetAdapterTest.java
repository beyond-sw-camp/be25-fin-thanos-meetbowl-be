package com.meetbowl.infrastructure.persistence.meeting;

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

import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.notification.MeetingReminderTarget;
import com.meetbowl.infrastructure.config.InfrastructureConfig;

/** 회의 리마인더 후보 조회(JPQL 생성자 표현식 투영)를 H2로 검증한다. 시작 시각 구간·SCHEDULED 상태 한정이 의도대로 동작하는지 확인한다. */
@SpringBootTest(classes = JpaMeetingReminderTargetAdapterTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:meeting-reminder-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class JpaMeetingReminderTargetAdapterTest {

    private static final Instant NOW = Instant.parse("2026-06-16T09:00:00Z");

    @Autowired private JpaMeetingReminderTargetAdapter adapter;
    @Autowired private JpaMeetingRepositoryAdapter meetingRepository;

    @Test
    void returnsOnlyScheduledMeetingsStartingWithinWindow() {
        // 구간 내 SCHEDULED — 포함 대상.
        Meeting inWindow = meetingRepository.save(scheduled("주간 회의", NOW.plusSeconds(3600)));
        // 구간 밖(상한 초과) — 제외.
        meetingRepository.save(scheduled("먼 회의", NOW.plusSeconds(36000)));
        // 과거 시작(하한 이하) — 제외.
        meetingRepository.save(scheduled("지난 회의", NOW.minusSeconds(3600)));
        // 구간 내지만 취소됨 — 제외.
        meetingRepository.save(scheduled("취소 회의", NOW.plusSeconds(3600)).cancel());

        List<MeetingReminderTarget> targets =
                adapter.findScheduledStartingWithin(NOW, NOW.plusSeconds(7200));

        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).meetingId()).isEqualTo(inWindow.id());
        assertThat(targets.get(0).title()).isEqualTo("주간 회의");
        assertThat(targets.get(0).scheduledAt()).isEqualTo(NOW.plusSeconds(3600));
    }

    private Meeting scheduled(String title, Instant scheduledAt) {
        return Meeting.create(
                title,
                scheduledAt,
                scheduledAt.plusSeconds(1800),
                UUID.randomUUID(),
                null,
                null,
                null,
                null);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        MeetingJpaConfig.class,
        JpaMeetingRepositoryAdapter.class,
        JpaMeetingReminderTargetAdapter.class
    })
    static class TestApplication {}
}
