package com.meetbowl.infrastructure.persistence.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.meetbowl.domain.meeting.AttendanceStatus;
import com.meetbowl.domain.meeting.AttendeeRole;
import com.meetbowl.domain.meeting.MeetingAttendee;

/** 회의록 색인 접근 범위가 실제 열람 가능한 참석자만 포함하는지 검증한다. */
class JpaMeetingMinutesAccessScopeAdapterTest {

    @Test
    void findReadableUserIdsExcludesDeclinedAttendeesAndDuplicates() {
        UUID meetingId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        UUID participantUserId = UUID.randomUUID();
        SpringDataMeetingAttendeeRepository repository =
                Mockito.mock(SpringDataMeetingAttendeeRepository.class);
        when(repository.findByMeetingId(meetingId))
                .thenReturn(
                        List.of(
                                entity(
                                        meetingId,
                                        hostUserId,
                                        AttendeeRole.HOST,
                                        AttendanceStatus.ACCEPTED),
                                entity(
                                        meetingId,
                                        hostUserId,
                                        AttendeeRole.REVIEWER,
                                        AttendanceStatus.INVITED),
                                entity(
                                        meetingId,
                                        participantUserId,
                                        AttendeeRole.PARTICIPANT,
                                        AttendanceStatus.DECLINED)));
        JpaMeetingMinutesAccessScopeAdapter adapter =
                new JpaMeetingMinutesAccessScopeAdapter(repository);

        assertEquals(List.of(hostUserId), adapter.findReadableUserIds(meetingId));
    }

    private MeetingAttendeeEntity entity(
            UUID meetingId, UUID userId, AttendeeRole role, AttendanceStatus attendanceStatus) {
        return MeetingAttendeeEntity.from(
                MeetingAttendee.of(UUID.randomUUID(), meetingId, userId, role, attendanceStatus));
    }
}
