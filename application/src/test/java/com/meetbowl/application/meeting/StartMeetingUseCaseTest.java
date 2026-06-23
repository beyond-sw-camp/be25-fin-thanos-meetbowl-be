package com.meetbowl.application.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.meeting.MeetingStatus;

class StartMeetingUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-12T01:00:00Z"), ZoneOffset.UTC);

    @Test
    void 회의채널이열리면예정회의를진행중으로변경한다() {
        UUID meetingId = UUID.randomUUID();
        StubMeetingRepository meetingRepository =
                new StubMeetingRepository(
                        Meeting.of(
                                meetingId,
                                "LiveKit 시작 테스트",
                                Instant.parse("2026-06-12T01:00:00Z"),
                                Instant.parse("2026-06-12T02:00:00Z"),
                                UUID.randomUUID(),
                                null,
                                "LIVEKIT",
                                "provider-room-1",
                                MeetingStatus.SCHEDULED,
                                null,
                                null,
                                null));

        StartMeetingUseCase useCase = new StartMeetingUseCase(meetingRepository, FIXED_CLOCK);

        useCase.execute(meetingId);

        Meeting saved = meetingRepository.savedMeeting;
        assertEquals(MeetingStatus.IN_PROGRESS, saved.status());
        assertEquals(Instant.parse("2026-06-12T01:00:00Z"), saved.startedAt());
    }

    @Test
    void 이미진행중인회의는멱등적으로무시한다() {
        UUID meetingId = UUID.randomUUID();
        Meeting inProgressMeeting =
                Meeting.of(
                        meetingId,
                        "진행중 회의",
                        Instant.parse("2026-06-12T01:00:00Z"),
                        Instant.parse("2026-06-12T02:00:00Z"),
                        UUID.randomUUID(),
                        null,
                        "LIVEKIT",
                        "provider-room-1",
                        MeetingStatus.IN_PROGRESS,
                        Instant.parse("2026-06-12T01:05:00Z"),
                        null,
                        null);
        StubMeetingRepository meetingRepository = new StubMeetingRepository(inProgressMeeting);

        StartMeetingUseCase useCase = new StartMeetingUseCase(meetingRepository, FIXED_CLOCK);

        useCase.execute(meetingId);

        assertFalse(meetingRepository.saveCalled);
    }

    @Test
    void 종료된회의는다시시작할수없다() {
        UUID meetingId = UUID.randomUUID();
        StubMeetingRepository meetingRepository =
                new StubMeetingRepository(
                        Meeting.of(
                                meetingId,
                                "종료된 회의",
                                Instant.parse("2026-06-12T01:00:00Z"),
                                Instant.parse("2026-06-12T02:00:00Z"),
                                UUID.randomUUID(),
                                null,
                                "LIVEKIT",
                                "provider-room-1",
                                MeetingStatus.ENDED,
                                Instant.parse("2026-06-12T01:05:00Z"),
                                Instant.parse("2026-06-12T02:00:00Z"),
                                null));

        StartMeetingUseCase useCase = new StartMeetingUseCase(meetingRepository, FIXED_CLOCK);

        BusinessException exception = assertThrows(BusinessException.class, () -> useCase.execute(meetingId));

        assertEquals(ErrorCode.MEETING_ALREADY_ENDED, exception.errorCode());
    }

    private static final class StubMeetingRepository implements MeetingRepositoryPort {
        private Meeting meeting;
        private Meeting savedMeeting;
        private boolean saveCalled;

        private StubMeetingRepository(Meeting meeting) {
            this.meeting = meeting;
        }

        @Override
        public Meeting save(Meeting meeting) {
            saveCalled = true;
            savedMeeting = meeting;
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
        public void deleteById(UUID id) {}
    }
}
