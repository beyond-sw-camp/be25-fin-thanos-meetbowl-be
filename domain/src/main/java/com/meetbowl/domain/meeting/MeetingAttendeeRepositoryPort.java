package com.meetbowl.domain.meeting;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 회의 참석자 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface MeetingAttendeeRepositoryPort {

    MeetingAttendee save(MeetingAttendee attendee);

    /** 한 회의의 참석자 일괄 저장(생성 시 배치 insert). */
    List<MeetingAttendee> saveAll(List<MeetingAttendee> attendees);

    List<MeetingAttendee> findByMeetingId(UUID meetingId);

    /**
     * 여러 회의의 참석자를 한 번에 배치 조회한다. 회의 목록 조회 시 회의별 참석자 쿼리를 반복(N+1)하지 않도록, 회의 id들을 모아 단일 {@code IN}
     * 쿼리로 조회한 뒤 호출 측에서 회의별로 그룹핑한다.
     */
    List<MeetingAttendee> findByMeetingIds(Collection<UUID> meetingIds);

    /** 내가 참석자로 지정된 회의 조회용. */
    List<MeetingAttendee> findByUserId(UUID userId);

    /**
     * 회의록 검토자 연동(T4-002)용: 해당 회의의 회의록 검토자(reviewer 플래그가 붙은 참석자) userId를 조회한다. 검토자를 지정하지 않은 회의는 빈
     * 값을 반환한다. 회의당 검토자는 최대 1명이다.
     */
    Optional<UUID> findReviewerUserId(UUID meetingId);

    void deleteByMeetingId(UUID meetingId);
}
