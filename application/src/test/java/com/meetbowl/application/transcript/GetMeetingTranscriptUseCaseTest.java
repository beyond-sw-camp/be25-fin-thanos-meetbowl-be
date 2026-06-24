package com.meetbowl.application.transcript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.domain.meeting.AttendanceStatus;
import com.meetbowl.domain.meeting.AttendeeRole;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.meeting.MeetingStatus;
import com.meetbowl.domain.transcript.MeetingTranscriptSegment;
import com.meetbowl.domain.transcript.MeetingTranscriptSegmentRepositoryPort;
import com.meetbowl.domain.transcript.TranscriptLanguage;

class GetMeetingTranscriptUseCaseTest {

    @Test
    void 참석자는회의원문리스트와전체텍스트를조회할수있다() {
        UUID meetingId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        UUID participantUserId = UUID.randomUUID();
        GetMeetingTranscriptUseCase useCase =
                new GetMeetingTranscriptUseCase(
                        new StubMeetingRepository(
                                Meeting.of(
                                        meetingId,
                                        "회의",
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        Instant.parse("2026-06-12T02:00:00Z"),
                                        hostUserId,
                                        null,
                                        "LIVEKIT",
                                        "room",
                                        MeetingStatus.ENDED,
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        Instant.parse("2026-06-12T02:00:00Z"),
                                        null)),
                        new StubAttendeeRepository(
                                List.of(
                                        MeetingAttendee.of(
                                                UUID.randomUUID(),
                                                meetingId,
                                                participantUserId,
                                                AttendeeRole.PARTICIPANT,
                                                AttendanceStatus.ACCEPTED))),
                        new StubTranscriptRepository(
                                List.of(
                                        MeetingTranscriptSegment.create(
                                                meetingId,
                                                "segment-1",
                                                1L,
                                                TranscriptLanguage.KO,
                                                "첫 문장",
                                                "첫 문장",
                                                "첫 문장",
                                                0L,
                                                500L,
                                                UUID.randomUUID()),
                                        MeetingTranscriptSegment.create(
                                                meetingId,
                                                "segment-2",
                                                2L,
                                                TranscriptLanguage.KO,
                                                "둘째 문장",
                                                "둘째 문장",
                                                "둘째 문장",
                                                600L,
                                                1000L,
                                                UUID.randomUUID()))));

        GetMeetingTranscriptResult result = useCase.execute(meetingId, participantUserId, false);

        assertEquals(2, result.segments().size());
        assertEquals("첫 문장\n둘째 문장", result.fullText());
    }

    @Test
    void 비참석자는회의원문을조회할수없다() {
        UUID meetingId = UUID.randomUUID();
        GetMeetingTranscriptUseCase useCase =
                new GetMeetingTranscriptUseCase(
                        new StubMeetingRepository(
                                Meeting.of(
                                        meetingId,
                                        "회의",
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        Instant.parse("2026-06-12T02:00:00Z"),
                                        UUID.randomUUID(),
                                        null,
                                        "LIVEKIT",
                                        "room",
                                        MeetingStatus.ENDED,
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        Instant.parse("2026-06-12T02:00:00Z"),
                                        null)),
                        new StubAttendeeRepository(List.of()),
                        new StubTranscriptRepository(List.of()));

        assertThrows(
                BusinessException.class,
                () -> useCase.execute(meetingId, UUID.randomUUID(), false));
    }

    private record StubMeetingRepository(Meeting meeting) implements MeetingRepositoryPort {
        @Override
        public Meeting save(Meeting meeting) {
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

    private record StubAttendeeRepository(List<MeetingAttendee> attendees)
            implements MeetingAttendeeRepositoryPort {
        @Override
        public MeetingAttendee save(MeetingAttendee attendee) {
            return attendee;
        }

        @Override
        public List<MeetingAttendee> saveAll(List<MeetingAttendee> attendees) {
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

    private record StubTranscriptRepository(List<MeetingTranscriptSegment> segments)
            implements MeetingTranscriptSegmentRepositoryPort {
        @Override
        public MeetingTranscriptSegment save(MeetingTranscriptSegment segment) {
            return segment;
        }

        @Override
        public boolean existsBySourceEventId(UUID sourceEventId) {
            return false;
        }

        @Override
        public List<MeetingTranscriptSegment> findAllByMeetingIdOrderBySequence(UUID meetingId) {
            return segments.stream()
                    .filter(segment -> segment.meetingId().equals(meetingId))
                    .toList();
        }
    }
}
