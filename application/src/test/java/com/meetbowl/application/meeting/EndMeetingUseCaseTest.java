package com.meetbowl.application.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.meeting.AttendanceStatus;
import com.meetbowl.domain.meeting.AttendeeRole;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.meeting.MeetingEndedEvent;
import com.meetbowl.domain.meeting.MeetingEndedEventOutboxPort;
import com.meetbowl.domain.meeting.MeetingRealtimeSessionStopper;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.meeting.MeetingStatus;

class EndMeetingUseCaseTest {

    @Test
    void scheduled회의도외부세션기준으로종료할수있다() {
        UUID meetingId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        UUID reviewerUserId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        RecordingMeetingEndedOutbox outbox = new RecordingMeetingEndedOutbox();
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
                                "provider-room-1",
                                MeetingStatus.SCHEDULED,
                                null,
                                null,
                                null));
        EndMeetingUseCase useCase =
                new EndMeetingUseCase(
                        meetingRepository,
                        new StubMeetingAttendeeRepository(
                                List.of(
                                        MeetingAttendee.of(
                                                UUID.randomUUID(),
                                                meetingId,
                                                reviewerUserId,
                                                AttendeeRole.PARTICIPANT,
                                                true,
                                                AttendanceStatus.ACCEPTED))),
                        hostId -> organizationId,
                        outbox,
                        new MeetingGuestNameAllocator(),
                        new RecordingRealtimeSessionStopper());

        EndMeetingResult result =
                useCase.execute(
                        new EndMeetingCommand(
                                meetingId,
                                Instant.parse("2026-06-12T02:00:00Z"),
                                UUID.randomUUID(),
                                "meeting_ended",
                                "stt-server"));

        assertEquals(MeetingStatus.ENDED.name(), result.status());
        assertTrue(result.meetingEndedEventQueued());
        assertEquals("주간 전략 회의", outbox.savedEvent.title());
        assertEquals(reviewerUserId, outbox.savedEvent.reviewerUserId());
        assertEquals(organizationId, outbox.savedEvent.organizationId());
    }

    @Test
    void 이미종료된회의는이벤트를다시발행하지않는다() {
        UUID meetingId = UUID.randomUUID();
        RecordingMeetingEndedOutbox outbox = new RecordingMeetingEndedOutbox();
        EndMeetingUseCase useCase =
                new EndMeetingUseCase(
                        new StubMeetingRepository(
                                Meeting.of(
                                        meetingId,
                                        "이미 종료된 회의",
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        Instant.parse("2026-06-12T02:00:00Z"),
                                        UUID.randomUUID(),
                                        null,
                                        "LIVEKIT",
                                        "provider-room-1",
                                        MeetingStatus.ENDED,
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        Instant.parse("2026-06-12T02:00:00Z"),
                                        null)),
                        new StubMeetingAttendeeRepository(List.of()),
                        hostId -> UUID.randomUUID(),
                        outbox,
                        new MeetingGuestNameAllocator(),
                        new RecordingRealtimeSessionStopper());

        EndMeetingResult result =
                useCase.execute(
                        new EndMeetingCommand(
                                meetingId,
                                Instant.now(),
                                UUID.randomUUID(),
                                "retry",
                                "stt-server"));

        assertFalse(result.meetingEndedEventQueued());
        assertFalse(outbox.called);
    }

    @Test
    void Outbox저장실패를성공으로숨기지않는다() {
        UUID meetingId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        StubMeetingRepository meetingRepository =
                new StubMeetingRepository(
                        Meeting.of(
                                meetingId,
                                "장애 회의",
                                Instant.parse("2026-06-12T01:00:00Z"),
                                Instant.parse("2026-06-12T02:00:00Z"),
                                hostUserId,
                                null,
                                "LIVEKIT",
                                "provider-room-1",
                                MeetingStatus.IN_PROGRESS,
                                Instant.parse("2026-06-12T01:05:00Z"),
                                null,
                                null));

        EndMeetingUseCase useCase =
                new EndMeetingUseCase(
                        meetingRepository,
                        new StubMeetingAttendeeRepository(
                                List.of(
                                        MeetingAttendee.of(
                                                UUID.randomUUID(),
                                                meetingId,
                                                UUID.randomUUID(),
                                                AttendeeRole.PARTICIPANT,
                                                true,
                                                AttendanceStatus.ACCEPTED))),
                        hostId -> UUID.randomUUID(),
                        new ThrowingMeetingEndedOutbox(),
                        new MeetingGuestNameAllocator(),
                        new RecordingRealtimeSessionStopper());

        assertThrows(
                IllegalStateException.class,
                () ->
                        useCase.execute(
                                new EndMeetingCommand(
                                        meetingId,
                                        Instant.parse("2026-06-12T02:00:00Z"),
                                        UUID.randomUUID(),
                                        "meeting_ended",
                                        "stt-server")));
    }

    private static final class StubMeetingRepository implements MeetingRepositoryPort {
        private Meeting meeting;

        private StubMeetingRepository(Meeting meeting) {
            this.meeting = meeting;
        }

        @Override
        public Meeting save(Meeting meeting) {
            this.meeting = meeting;
            return meeting;
        }

        @Override
        public Optional<Meeting> findById(UUID id) {
            return Optional.ofNullable(meeting).filter(value -> value.id().equals(id));
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
        public List<com.meetbowl.domain.meeting.AttendeeConflict> findActiveByAttendees(
                java.util.Collection<UUID> userIds,
                Instant from,
                Instant to,
                UUID excludeMeetingId) {
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
        public List<MeetingAttendee> findByUserId(UUID userId) {
            return attendees.stream().filter(attendee -> attendee.userId().equals(userId)).toList();
        }

        @Override
        public List<MeetingAttendee> findByMeetingIds(java.util.Collection<UUID> meetingIds) {
            return attendees.stream()
                    .filter(attendee -> meetingIds.contains(attendee.meetingId()))
                    .toList();
        }

        @Override
        public Optional<UUID> findReviewerUserId(UUID meetingId) {
            return findByMeetingId(meetingId).stream()
                    .filter(MeetingAttendee::reviewer)
                    .map(MeetingAttendee::userId)
                    .findFirst();
        }

        @Override
        public void deleteByMeetingId(UUID meetingId) {}
    }

    private static final class RecordingMeetingEndedOutbox implements MeetingEndedEventOutboxPort {
        private boolean called;
        private MeetingEndedEvent savedEvent;

        @Override
        public void save(MeetingEndedEvent event) {
            this.called = true;
            this.savedEvent = event;
        }

        @Override
        public List<MeetingEndedEvent> findReadyToPublish(Instant now, int limit) {
            return List.of();
        }

        @Override
        public void removePublished(UUID eventId) {}

        @Override
        public void markFailed(UUID eventId, Instant nextAttemptAt, String failureReason) {}
    }

    private static final class ThrowingMeetingEndedOutbox implements MeetingEndedEventOutboxPort {

        @Override
        public void save(MeetingEndedEvent event) {
            throw new IllegalStateException("outbox unavailable");
        }

        @Override
        public List<MeetingEndedEvent> findReadyToPublish(Instant now, int limit) {
            return List.of();
        }

        @Override
        public void removePublished(UUID eventId) {}

        @Override
        public void markFailed(UUID eventId, Instant nextAttemptAt, String failureReason) {}
    }

    private static final class RecordingRealtimeSessionStopper
            implements MeetingRealtimeSessionStopper {

        @Override
        public void stop(UUID meetingId) {}
    }
}
