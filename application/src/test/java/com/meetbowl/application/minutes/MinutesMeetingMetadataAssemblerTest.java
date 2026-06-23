package com.meetbowl.application.minutes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.meeting.AttendeeRole;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.organization.DepartmentRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

class MinutesMeetingMetadataAssemblerTest {

    @Test
    void assemblesReviewerWithoutDepartment() {
        UUID meetingId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        UUID reviewerUserId = UUID.randomUUID();
        Instant scheduledAt = Instant.parse("2099-01-01T01:00:00Z");
        Meeting meeting =
                Meeting.create(
                        "회의록 테스트",
                        scheduledAt,
                        scheduledAt.plusSeconds(1800),
                        hostUserId,
                        null,
                        "LIVEKIT",
                        null,
                        "검토자 부서 미배정 테스트");
        Meeting savedMeeting =
                Meeting.of(
                        meetingId,
                        meeting.title(),
                        meeting.scheduledAt(),
                        meeting.scheduledEndAt(),
                        meeting.hostUserId(),
                        meeting.meetingRoomId(),
                        meeting.provider(),
                        meeting.providerRoomId(),
                        meeting.status(),
                        meeting.startedAt(),
                        meeting.endedAt(),
                        meeting.description());
        MeetingAttendee reviewer =
                MeetingAttendee.create(meetingId, reviewerUserId, AttendeeRole.PARTICIPANT, true);
        User host = user(hostUserId, organizationId, "host", "호스트");
        User reviewerUser = user(reviewerUserId, organizationId, "reviewer", "검토자");

        MeetingRepositoryPort meetingRepositoryPort = mock(MeetingRepositoryPort.class);
        MeetingAttendeeRepositoryPort attendeeRepositoryPort =
                mock(MeetingAttendeeRepositoryPort.class);
        UserRepositoryPort userRepositoryPort = mock(UserRepositoryPort.class);
        DepartmentRepositoryPort departmentRepositoryPort = mock(DepartmentRepositoryPort.class);
        when(meetingRepositoryPort.findByIds(List.of(meetingId))).thenReturn(List.of(savedMeeting));
        when(attendeeRepositoryPort.findByMeetingIds(List.of(meetingId))).thenReturn(List.of(reviewer));
        when(userRepositoryPort.findAllByAffiliateId(organizationId))
                .thenReturn(List.of(host, reviewerUser));

        MinutesMeetingMetadataAssembler assembler =
                new MinutesMeetingMetadataAssembler(
                        meetingRepositoryPort,
                        attendeeRepositoryPort,
                        userRepositoryPort,
                        departmentRepositoryPort);

        Map<UUID, MinutesMeetingMetadata> result = assembler.assemble(List.of(meetingId), organizationId);

        MinutesMeetingMetadata metadata = result.get(meetingId);
        assertEquals("회의록 테스트", metadata.title());
        assertEquals(1, metadata.attendeeCount());
        assertEquals(reviewerUserId, metadata.reviewerUserId());
        assertEquals("검토자", metadata.reviewerName());
        assertNull(metadata.reviewerDepartment());
        verifyNoInteractions(departmentRepositoryPort);
    }

    private static User user(UUID id, UUID organizationId, String loginId, String name) {
        return User.of(
                id,
                loginId,
                "{noop}1234",
                name,
                loginId + "@meetbowl.local",
                UserRole.USER,
                UserStatus.ACTIVE,
                organizationId,
                null,
                null,
                null,
                false,
                null,
                null,
                Instant.parse("2099-01-01T00:00:00Z"),
                Instant.parse("2099-01-01T00:00:00Z"));
    }
}
