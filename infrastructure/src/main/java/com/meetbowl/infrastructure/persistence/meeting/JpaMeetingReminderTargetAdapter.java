package com.meetbowl.infrastructure.persistence.meeting;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.meeting.MeetingStatus;
import com.meetbowl.domain.notification.MeetingReminderTarget;
import com.meetbowl.domain.notification.MeetingReminderTargetPort;

/**
 * 회의 리마인더 읽기 포트({@link MeetingReminderTargetPort})의 JPA 구현이다.
 *
 * <p>발송 후보는 아직 시작 전인 예약 회의이므로 상태 SCHEDULED만 조회한다. 회의 도메인/엔티티 변환은 Spring Data가 투영으로 직접 만들어 반환하므로 이
 * 어댑터는 상태 조건 고정과 위임만 담당한다.
 */
@Repository
public class JpaMeetingReminderTargetAdapter implements MeetingReminderTargetPort {

    private final SpringDataMeetingReminderRepository repository;

    public JpaMeetingReminderTargetAdapter(SpringDataMeetingReminderRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MeetingReminderTarget> findScheduledStartingWithin(Instant from, Instant to) {
        return repository.findScheduledStartingWithin(MeetingStatus.SCHEDULED, from, to);
    }
}