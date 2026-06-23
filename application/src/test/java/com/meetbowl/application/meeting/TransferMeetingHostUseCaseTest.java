package com.meetbowl.application.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.AttendanceStatus;
import com.meetbowl.domain.meeting.AttendeeRole;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.meeting.MeetingStatus;

class TransferMeetingHostUseCaseTest {

    @Test
    void 회의관리자를다른참석자로이전한다() {
        UUID meetingId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        UUID newHostUserId = UUID.randomUUID();
        UUID participantUserId = UUID.randomUUID();
        StubMeetingRepository meetingRepository =
                new StubMeetingRepository(
                        Meeting.of(
                                meetingId,
                                "주간 전략 회의",
                                Instant.parse("2026-06-12T01:00:00Z"),
                                Instant.parse("2026-06-12T02:00:00Z"),
                                hostUserId,
                                null,
                                "LIVEKIT",
                                "provider-room-123",
                                MeetingStatus.IN_PROGRESS,
                                Instant.parse("2026-06-12T01:00:00Z"),
                                null,
                                "회의 본문"));
        StubMeetingAttendeeRepository attendeeRepository =
                new StubMeetingAttendeeRepository(
                        List.of(
                                MeetingAttendee.of(
                                        UUID.randomUUID(),
                                        meetingId,
                                        hostUserId,
                                        AttendeeRole.HOST,
                                        AttendanceStatus.ACCEPTED),
                                MeetingAttendee.of(
                                        UUID.randomUUID(),
                                        meetingId,
                                        newHostUserId,
                                        AttendeeRole.PARTICIPANT,
                                        AttendanceStatus.ACCEPTED),
                                MeetingAttendee.of(
                                        UUID.randomUUID(),
                                        meetingId,
                                        participantUserId,
                                        AttendeeRole.PARTICIPANT,
                                        AttendanceStatus.ACCEPTED)));

        TransferMeetingHostUseCase useCase =
                new TransferMeetingHostUseCase(meetingRepository, attendeeRepository);

        MeetingResult result =
                useCase.execute(new TransferMeetingHostCommand(meetingId, hostUserId, newHostUserId));

        assertEquals(newHostUserId, result.hostUserId());
        assertEquals(newHostUserId, meetingRepository.savedMeeting.hostUserId());
        assertEquals(AttendeeRole.PARTICIPANT, attendeeRepository.attendees.get(0).role());
        assertEquals(AttendeeRole.HOST, attendeeRepository.attendees.get(1).role());
        assertEquals(AttendeeRole.PARTICIPANT, attendeeRepository.attendees.get(2).role());
    }

    @Test
    void 호스트가아니면이전할수없다() {
        UUID meetingId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();
        UUID newHostUserId = UUID.randomUUID();
        TransferMeetingHostUseCase useCase =
                new TransferMeetingHostUseCase(
                        new StubMeetingRepository(
                                Meeting.of(
                                        meetingId,
                                        "주간 전략 회의",
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        Instant.parse("2026-06-12T02:00:00Z"),
                                        hostUserId,
                                        null,
                                        "LIVEKIT",
                                        "provider-room-123",
                                        MeetingStatus.IN_PROGRESS,
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        null,
                                        "회의 본문")),
                        new StubMeetingAttendeeRepository(
                                List.of(
                                        MeetingAttendee.of(
                                                UUID.randomUUID(),
                                                meetingId,
                                                hostUserId,
                                                AttendeeRole.HOST,
                                                AttendanceStatus.ACCEPTED),
                                        MeetingAttendee.of(
                                                UUID.randomUUID(),
                                                meetingId,
                                                newHostUserId,
                                                AttendeeRole.PARTICIPANT,
                                                AttendanceStatus.ACCEPTED))));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new TransferMeetingHostCommand(
                                                meetingId, requesterUserId, newHostUserId)));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
    }

    private static final class StubMeetingRepository implements MeetingRepositoryPort {
        private Meeting savedMeeting;

        private StubMeetingRepository(Meeting meeting) {
            this.savedMeeting = meeting;
        }

        @Override
        public Meeting save(Meeting meeting) {
            this.savedMeeting = meeting;
            return meeting;
        }

        @Override
        public Optional<Meeting> findById(UUID id) {
            return Optional.ofNullable(savedMeeting).filter(value -> value.id().equals(id));
        }

        @Override
        public List<Meeting> findByHostUserId(UUID hostUserId) {
            return List.of();
        }

        @Override
        public List<Meeting> findActiveRoomOverlaps(
                UUID meetingRoomId, Instant scheduledStartAt, Instant scheduledEndAt) {
            return List.of();
        }

        @Override
        public List<Meeting> findActiveOverlapsInRooms(
                List<UUID> meetingRoomIds, Instant from, Instant to) {
            return List.of();
        }

        @Override
        public List<Meeting> findNonCancelledRoomMeetingsOverlapping(Instant from, Instant to) {
            return List.of();
        }

        @Override
        public void deleteById(UUID id) {}
    }

    private static final class StubMeetingAttendeeRepository
            implements MeetingAttendeeRepositoryPort {
        private final List<MeetingAttendee> attendees;

        private StubMeetingAttendeeRepository(List<MeetingAttendee> attendees) {
            this.attendees = new ArrayList<>(attendees);
        }

        @Override
        public MeetingAttendee save(MeetingAttendee attendee) {
            attendees.add(attendee);
            return attendee;
        }

        @Override
        public List<MeetingAttendee> saveAll(List<MeetingAttendee> attendees) {
            this.attendees.clear();
            this.attendees.addAll(attendees);
            return attendees;
        }

        @Override
        public List<MeetingAttendee> findByMeetingId(UUID meetingId) {
            return attendees.stream()
                    .filter(attendee -> attendee.meetingId().equals(meetingId))
                    .toList();
        }

        @Override
        public List<MeetingAttendee> findByMeetingIds(java.util.Collection<UUID> meetingIds) {
            return attendees.stream()
                    .filter(attendee -> meetingIds.contains(attendee.meetingId()))
                    .toList();
        }

        @Override
        public List<MeetingAttendee> findByUserId(UUID userId) {
            return attendees.stream().filter(attendee -> attendee.userId().equals(userId)).toList();
        }

        @Override
        public Optional<UUID> findReviewerUserId(UUID meetingId) {
            return Optional.empty();
        }

        @Override
        public void deleteByMeetingId(UUID meetingId) {}
    }
}
