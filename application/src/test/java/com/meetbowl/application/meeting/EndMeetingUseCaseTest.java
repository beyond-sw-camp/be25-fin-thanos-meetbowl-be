package com.meetbowl.application.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.meeting.MeetingStatus;

class EndMeetingUseCaseTest {

    @Test
    void scheduled회의도외부세션기준으로종료할수있다() {
        UUID meetingId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        UUID reviewerUserId = UUID.randomUUID();
        RecordingMeetingEndedEventPublisher eventPublisher =
                new RecordingMeetingEndedEventPublisher();
        StubMeetingRepository meetingRepository =
                new StubMeetingRepository(
                        Meeting.of(
                                meetingId,
                                "주간 전략 회의",
                                Instant.parse("2026-06-12T01:00:00Z"),
                                hostUserId,
                                null,
                                "LIVEKIT",
                                "provider-room-1",
                                MeetingStatus.SCHEDULED,
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
                                                AttendeeRole.REVIEWER,
                                                AttendanceStatus.ACCEPTED))),
                        eventPublisher,
                        new MeetingGuestNameAllocator());

        EndMeetingResult result =
                useCase.execute(
                        new EndMeetingCommand(
                                meetingId,
                                Instant.parse("2026-06-12T02:00:00Z"),
                                UUID.randomUUID(),
                                "meeting_ended",
                                "stt-server"));

        assertEquals(MeetingStatus.ENDED.name(), result.status());
        assertTrue(result.meetingEndedEventPublished());
        assertEquals("주간 전략 회의", eventPublisher.title);
        assertEquals(reviewerUserId, eventPublisher.reviewerUserId);
    }

    @Test
    void 이미종료된회의는이벤트를다시발행하지않는다() {
        UUID meetingId = UUID.randomUUID();
        RecordingMeetingEndedEventPublisher eventPublisher =
                new RecordingMeetingEndedEventPublisher();
        EndMeetingUseCase useCase =
                new EndMeetingUseCase(
                        new StubMeetingRepository(
                                Meeting.of(
                                        meetingId,
                                        "이미 종료된 회의",
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        UUID.randomUUID(),
                                        null,
                                        "LIVEKIT",
                                        "provider-room-1",
                                        MeetingStatus.ENDED,
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        Instant.parse("2026-06-12T02:00:00Z"))),
                        new StubMeetingAttendeeRepository(List.of()),
                        eventPublisher,
                        new MeetingGuestNameAllocator());

        EndMeetingResult result =
                useCase.execute(
                        new EndMeetingCommand(
                                meetingId,
                                Instant.now(),
                                UUID.randomUUID(),
                                "retry",
                                "stt-server"));

        assertFalse(result.meetingEndedEventPublished());
        assertFalse(eventPublisher.called);
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
        public void deleteByMeetingId(UUID meetingId) {}
    }

    private static final class RecordingMeetingEndedEventPublisher
            implements MeetingEndedEventPublisher {
        private boolean called;
        private UUID reviewerUserId;
        private String title;

        @Override
        public void publishMeetingEnded(
                UUID meetingId,
                UUID hostUserId,
                UUID reviewerUserId,
                String title,
                Instant startedAt,
                Instant endedAt,
                UUID correlationId) {
            this.called = true;
            this.reviewerUserId = reviewerUserId;
            this.title = title;
        }
    }
}
