package com.meetbowl.domain.meeting;

import java.util.List;
import java.util.UUID;

/** 회의 참석자 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface MeetingAttendeeRepositoryPort {

    MeetingAttendee save(MeetingAttendee attendee);

    /** 한 회의의 참석자 일괄 저장(생성 시 배치 insert). */
    List<MeetingAttendee> saveAll(List<MeetingAttendee> attendees);

    List<MeetingAttendee> findByMeetingId(UUID meetingId);

    /** 내가 참석자로 지정된 회의 조회용. */
    List<MeetingAttendee> findByUserId(UUID userId);

    void deleteByMeetingId(UUID meetingId);
}
