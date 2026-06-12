package com.meetbowl.infrastructure.persistence.meeting;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.document.MeetingMinutesAccessScopePort;
import com.meetbowl.domain.meeting.AttendanceStatus;

/** 회의 참석자 저장소를 기준으로 승인 회의록의 사용자 열람 범위를 계산하는 adapter다. */
@Repository
public class JpaMeetingMinutesAccessScopeAdapter implements MeetingMinutesAccessScopePort {

    private final SpringDataMeetingAttendeeRepository meetingAttendeeRepository;

    public JpaMeetingMinutesAccessScopeAdapter(
            SpringDataMeetingAttendeeRepository meetingAttendeeRepository) {
        this.meetingAttendeeRepository = meetingAttendeeRepository;
    }

    @Override
    public List<UUID> findReadableUserIds(UUID meetingId) {
        // 참석을 명시적으로 거절한 사용자는 과거 회의록 검색 범위에 포함하지 않는다.
        return meetingAttendeeRepository.findByMeetingId(meetingId).stream()
                .map(MeetingAttendeeEntity::toDomain)
                .filter(attendee -> attendee.attendanceStatus() != AttendanceStatus.DECLINED)
                .map(attendee -> attendee.userId())
                .distinct()
                .toList();
    }
}
