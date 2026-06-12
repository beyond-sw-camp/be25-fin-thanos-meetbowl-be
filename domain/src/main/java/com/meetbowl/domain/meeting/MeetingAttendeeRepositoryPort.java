package com.meetbowl.domain.meeting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 회의 참석자 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface MeetingAttendeeRepositoryPort {

    MeetingAttendee save(MeetingAttendee attendee);

    /** 한 회의의 참석자 일괄 저장(생성 시 배치 insert). */
    List<MeetingAttendee> saveAll(List<MeetingAttendee> attendees);

    List<MeetingAttendee> findByMeetingId(UUID meetingId);

    /** 내가 참석자로 지정된 회의 조회용. */
    List<MeetingAttendee> findByUserId(UUID userId);

    /**
     * 회의록 검토자 연동(T4-002)용: 해당 회의의 회의록 검토자(REVIEWER 역할 참석자) userId를 조회한다. 검토자를 지정하지 않은 회의는 빈 값을
     * 반환한다. 회의당 검토자는 최대 1명이다.
     */
    Optional<UUID> findReviewerUserId(UUID meetingId);

    void deleteByMeetingId(UUID meetingId);
}
