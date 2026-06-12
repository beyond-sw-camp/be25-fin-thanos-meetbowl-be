package com.meetbowl.application.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.meeting.LiveKitTokenIssueCommand;
import com.meetbowl.domain.meeting.LiveKitTokenIssueResult;
import com.meetbowl.domain.meeting.LiveKitTokenIssuer;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.meeting.MeetingStatus;

class JoinMeetingUseCaseTest {

    @Test
    void providerRoomId가있으면기존Room으로토큰을발급한다() {
        UUID meetingId = UUID.randomUUID();
        RecordingTokenIssuer tokenIssuer = new RecordingTokenIssuer();
        JoinMeetingUseCase useCase =
                new JoinMeetingUseCase(
                        new StubMeetingRepository(
                                Meeting.of(
                                        meetingId,
                                        "주간 전략 회의",
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        UUID.randomUUID(),
                                        null,
                                        "LIVEKIT",
                                        "provider-room-123",
                                        MeetingStatus.SCHEDULED,
                                        null,
                                        null)),
                        tokenIssuer);

        JoinMeetingResult result =
                useCase.execute(
                        new JoinMeetingCommand(
                                meetingId,
                                UUID.fromString("31f73d71-c04e-4410-a98c-fdc15e918091"),
                                "이지연",
                                "ignored-client-identity"));

        assertEquals("provider-room-123", result.roomName());
        assertEquals("user-31f73d71-c04e-4410-a98c-fdc15e918091", result.participantIdentity());
        assertEquals("이지연", result.participantName());
        assertEquals("provider-room-123", tokenIssuer.lastCommand.roomName());
    }

    @Test
    void 회의가없으면meetingId기반FallbackRoom을사용한다() {
        UUID meetingId = UUID.fromString("3ef5f58f-50b2-4f0b-97bf-42e79d91ac39");
        RecordingTokenIssuer tokenIssuer = new RecordingTokenIssuer();
        JoinMeetingUseCase useCase =
                new JoinMeetingUseCase(new StubMeetingRepository(null), tokenIssuer);

        JoinMeetingResult result =
                useCase.execute(
                        new JoinMeetingCommand(
                                meetingId, null, "livekit-test", "frontend-participant"));

        assertEquals("meeting-3ef5f58f-50b2-4f0b-97bf-42e79d91ac39", result.roomName());
        assertEquals("frontend-participant", result.participantIdentity());
        assertEquals("frontend-participant", tokenIssuer.lastCommand.participantIdentity());
    }

    private static final class RecordingTokenIssuer implements LiveKitTokenIssuer {

        private LiveKitTokenIssueCommand lastCommand;

        @Override
        public LiveKitTokenIssueResult issue(LiveKitTokenIssueCommand command) {
            this.lastCommand = command;
            return new LiveKitTokenIssueResult(
                    "http://localhost:7880",
                    "issued-token",
                    Instant.parse("2026-06-12T01:00:00Z"),
                    Instant.parse("2026-06-12T02:00:00Z"));
        }
    }

    private static final class StubMeetingRepository implements MeetingRepositoryPort {

        private final Meeting meeting;

        private StubMeetingRepository(Meeting meeting) {
            this.meeting = meeting;
        }

        @Override
        public Meeting save(Meeting meeting) {
            return meeting;
        }

        @Override
        public Optional<Meeting> findById(UUID id) {
            return Optional.ofNullable(meeting).filter(savedMeeting -> savedMeeting.id().equals(id));
        }

        @Override
        public List<Meeting> findByHostUserId(UUID hostUserId) {
            return List.of();
        }

        @Override
        public void deleteById(UUID id) {}
    }
}
