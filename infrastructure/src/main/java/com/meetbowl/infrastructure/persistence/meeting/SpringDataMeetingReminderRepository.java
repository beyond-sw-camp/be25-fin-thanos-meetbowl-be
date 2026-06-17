package com.meetbowl.infrastructure.persistence.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.meetbowl.domain.meeting.MeetingStatus;
import com.meetbowl.domain.notification.MeetingReminderTarget;

/**
 * 회의 리마인더 발송 후보를 회의 테이블에서 투영으로 읽는 Spring Data repository다.
 *
 * <p>알림 스케줄러를 위한 조회지만, {@code MeetingEntity}를 대상으로 하므로 회의 영속 패키지에 둔다(엔티티 스캔 범위 안에서만 메타모델이 해석된다). 회의
 * 본체를 끌어오지 않고 JPQL 생성자 표현식으로 {@link MeetingReminderTarget} 투영만 조회한다. 회의 팀 소유의 {@code
 * SpringDataMeetingRepository}는 수정하지 않고 별도 인터페이스로 추가한다.
 */
interface SpringDataMeetingReminderRepository extends Repository<MeetingEntity, UUID> {

    /**
     * 예정 시작 시각이 ({@code from}, {@code to}] 구간에 드는 SCHEDULED 회의를 시작 시각 오름차순으로 조회한다. 취소/진행/종료된 회의는
     * 제외된다.
     */
    @Query(
            "select new com.meetbowl.domain.notification.MeetingReminderTarget(m.id, m.title,"
                    + " m.scheduledAt)"
                    + " from MeetingEntity m"
                    + " where m.status = :status"
                    + " and m.scheduledAt > :from"
                    + " and m.scheduledAt <= :to"
                    + " order by m.scheduledAt asc")
    List<MeetingReminderTarget> findScheduledStartingWithin(
            @Param("status") MeetingStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
