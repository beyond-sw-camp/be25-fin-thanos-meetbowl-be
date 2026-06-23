package com.meetbowl.application.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.LiveKitTokenIssueCommand;
import com.meetbowl.domain.meeting.LiveKitTokenIssueResult;
import com.meetbowl.domain.meeting.LiveKitTokenIssuer;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingRealtimeSessionStarter;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.meeting.MeetingStatus;

class JoinMeetingUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-12T00:40:00Z"), ZoneOffset.UTC);

    @Test
    void providerRoomId가있으면기존Room으로토큰을발급한다() {
        UUID meetingId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        RecordingTokenIssuer tokenIssuer = new RecordingTokenIssuer();
        RecordingRealtimeSessionStarter realtimeSessionStarter =
                new RecordingRealtimeSessionStarter();
        JoinMeetingUseCase useCase =
                new JoinMeetingUseCase(
                        new StubMeetingRepository(
                                Meeting.of(
                                        meetingId,
                                        "주간 전략 회의",
                                        Instant.parse("2026-06-12T00:50:00Z"),
                                        Instant.parse("2026-06-12T01:50:00Z"),
                                        hostUserId,
                                        null,
                                        "LIVEKIT",
                                        "provider-room-123",
                                        MeetingStatus.SCHEDULED,
                                        null,
                                        null,
                                        "주간 전략 공유")),
                        tokenIssuer,
                        realtimeSessionStarter,
                        new MeetingGuestNameAllocator(),
                        FIXED_CLOCK);

        JoinMeetingResult result =
                useCase.execute(
                        new JoinMeetingCommand(
                                meetingId,
                                UUID.fromString("31f73d71-c04e-4410-a98c-fdc15e918091"),
                                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                                "이지연",
                                "ignored-client-identity"));

        assertEquals("provider-room-123", result.roomName());
        assertEquals(hostUserId, result.hostUserId());
        assertEquals("user-31f73d71-c04e-4410-a98c-fdc15e918091", result.participantIdentity());
        assertEquals("이지연", result.participantName());
        assertEquals("provider-room-123", tokenIssuer.lastCommand.roomName());
        assertEquals(meetingId, realtimeSessionStarter.lastMeetingId);
        assertEquals(
                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                realtimeSessionStarter.lastOrganizationId);
        assertEquals("provider-room-123", realtimeSessionStarter.lastRoomName);
    }

    @Test
    void 회의가없으면meetingId기반FallbackRoom을사용한다() {
        UUID meetingId = UUID.fromString("3ef5f58f-50b2-4f0b-97bf-42e79d91ac39");
        RecordingTokenIssuer tokenIssuer = new RecordingTokenIssuer();
        RecordingRealtimeSessionStarter realtimeSessionStarter =
                new RecordingRealtimeSessionStarter();
        JoinMeetingUseCase useCase =
                new JoinMeetingUseCase(
                        new StubMeetingRepository(null),
                        tokenIssuer,
                        realtimeSessionStarter,
                        new MeetingGuestNameAllocator(),
                        FIXED_CLOCK);

        JoinMeetingResult result =
                useCase.execute(
                        new JoinMeetingCommand(
                                meetingId, null, "livekit-test", "frontend-participant"));

        assertEquals("meeting-3ef5f58f-50b2-4f0b-97bf-42e79d91ac39", result.roomName());
        assertEquals("frontend-participant", result.participantIdentity());
        assertEquals("frontend-participant", tokenIssuer.lastCommand.participantIdentity());
        assertTrue(!realtimeSessionStarter.called);
    }

    @Test
    void 회의입장시Stt실시간세션을먼저보장한다() {
        UUID meetingId = UUID.randomUUID();
        RecordingTokenIssuer tokenIssuer = new RecordingTokenIssuer();
        RecordingRealtimeSessionStarter realtimeSessionStarter =
                new RecordingRealtimeSessionStarter();
        JoinMeetingUseCase useCase =
                new JoinMeetingUseCase(
                        new StubMeetingRepository(null),
                        tokenIssuer,
                        realtimeSessionStarter,
                        new MeetingGuestNameAllocator(),
                        FIXED_CLOCK);

        useCase.execute(
                new JoinMeetingCommand(
                        meetingId,
                        UUID.randomUUID(),
                        UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                        "자동 자막 테스트",
                        "frontend-participant"));

        assertTrue(realtimeSessionStarter.called);
        assertEquals(
                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                realtimeSessionStarter.lastOrganizationId);
        assertEquals("meeting-" + meetingId, realtimeSessionStarter.lastRoomName);
    }

    @Test
    void 게스트는기본이름대신번호가붙은이름을받는다() {
        UUID meetingId = UUID.randomUUID();
        RecordingTokenIssuer tokenIssuer = new RecordingTokenIssuer();
        RecordingRealtimeSessionStarter realtimeSessionStarter =
                new RecordingRealtimeSessionStarter();
        JoinMeetingUseCase useCase =
                new JoinMeetingUseCase(
                        new StubMeetingRepository(null),
                        tokenIssuer,
                        realtimeSessionStarter,
                        new MeetingGuestNameAllocator(),
                        FIXED_CLOCK);

        JoinMeetingResult result =
                useCase.execute(
                        new JoinMeetingCommand(
                                meetingId, null, "", "guest-12345678-1234-1234-1234-123456789012"));

        assertTrue(result.participantName().startsWith("게스트 "));
        assertEquals(result.participantName(), tokenIssuer.lastCommand.participantName());
        assertTrue(!realtimeSessionStarter.called);
    }

    @Test
    void 같은회의의게스트는입장순서대로순번을받는다() {
        UUID meetingId = UUID.randomUUID();
        RecordingTokenIssuer tokenIssuer = new RecordingTokenIssuer();
        RecordingRealtimeSessionStarter realtimeSessionStarter =
                new RecordingRealtimeSessionStarter();
        MeetingGuestNameAllocator allocator = new MeetingGuestNameAllocator();
        JoinMeetingUseCase useCase =
                new JoinMeetingUseCase(
                        new StubMeetingRepository(null),
                        tokenIssuer,
                        realtimeSessionStarter,
                        allocator,
                        FIXED_CLOCK);

        JoinMeetingResult first =
                useCase.execute(new JoinMeetingCommand(meetingId, null, "", "guest-1"));
        JoinMeetingResult second =
                useCase.execute(new JoinMeetingCommand(meetingId, null, "", "guest-2"));

        assertEquals("게스트 1", first.participantName());
        assertEquals("게스트 2", second.participantName());
    }

    @Test
    void 종료된회의에는재입장할수없다() {
        UUID meetingId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        JoinMeetingUseCase useCase =
                new JoinMeetingUseCase(
                        new StubMeetingRepository(
                                Meeting.of(
                                        meetingId,
                                        "종료된 회의",
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        Instant.parse("2026-06-12T02:00:00Z"),
                                        hostUserId,
                                        null,
                                        "LIVEKIT",
                                        "provider-room-ended",
                                        MeetingStatus.ENDED,
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        Instant.parse("2026-06-12T02:00:00Z"),
                                        "종료 테스트")),
                        new RecordingTokenIssuer(),
                        new RecordingRealtimeSessionStarter(),
                        new MeetingGuestNameAllocator(),
                        FIXED_CLOCK);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new JoinMeetingCommand(
                                                meetingId, null, "게스트", "guest-ended-test")));

        assertEquals(ErrorCode.MEETING_ALREADY_ENDED, exception.errorCode());
    }

    @Test
    void 회의시작15분전보다이르면입장할수없다() {
        UUID meetingId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        JoinMeetingUseCase useCase =
                new JoinMeetingUseCase(
                        new StubMeetingRepository(
                                Meeting.of(
                                        meetingId,
                                        "너무 이른 회의 입장",
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        Instant.parse("2026-06-12T02:00:00Z"),
                                        hostUserId,
                                        null,
                                        "LIVEKIT",
                                        "provider-room-upcoming",
                                        MeetingStatus.SCHEDULED,
                                        null,
                                        null,
                                        "입장 시간 제한 테스트")),
                        new RecordingTokenIssuer(),
                        new RecordingRealtimeSessionStarter(),
                        new MeetingGuestNameAllocator(),
                        FIXED_CLOCK);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new JoinMeetingCommand(
                                                meetingId, null, "게스트", "guest-early-test")));

        assertEquals(ErrorCode.MEETING_JOIN_TOO_EARLY, exception.errorCode());
    }

    @Test
    void 회의시작15분전부터는입장할수있다() {
        UUID meetingId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        RecordingTokenIssuer tokenIssuer = new RecordingTokenIssuer();
        RecordingRealtimeSessionStarter realtimeSessionStarter =
                new RecordingRealtimeSessionStarter();
        JoinMeetingUseCase useCase =
                new JoinMeetingUseCase(
                        new StubMeetingRepository(
                                Meeting.of(
                                        meetingId,
                                        "입장 가능 회의",
                                        Instant.parse("2026-06-12T00:55:00Z"),
                                        Instant.parse("2026-06-12T01:55:00Z"),
                                        hostUserId,
                                        null,
                                        "LIVEKIT",
                                        "provider-room-open",
                                        MeetingStatus.SCHEDULED,
                                        null,
                                        null,
                                        "입장 가능 시간 테스트")),
                        tokenIssuer,
                        realtimeSessionStarter,
                        new MeetingGuestNameAllocator(),
                        FIXED_CLOCK);

        JoinMeetingResult result =
                useCase.execute(new JoinMeetingCommand(meetingId, null, "게스트", "guest-open-test"));

        assertEquals("provider-room-open", result.roomName());
        assertTrue(!realtimeSessionStarter.called);
    }

    @Test
    void 취소된회의는입장할수없다() {
        UUID meetingId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        JoinMeetingUseCase useCase =
                new JoinMeetingUseCase(
                        new StubMeetingRepository(
                                Meeting.of(
                                        meetingId,
                                        "취소된 회의",
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        Instant.parse("2026-06-12T02:00:00Z"),
                                        hostUserId,
                                        null,
                                        "LIVEKIT",
                                        "provider-room-cancelled",
                                        MeetingStatus.CANCELLED,
                                        null,
                                        null,
                                        "취소 회의 입장 차단 테스트")),
                        new RecordingTokenIssuer(),
                        new RecordingRealtimeSessionStarter(),
                        new MeetingGuestNameAllocator(),
                        FIXED_CLOCK);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new JoinMeetingCommand(
                                                meetingId, null, "게스트", "guest-cancelled-test")));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void stt세션자동시작이실패해도회의입장은계속할수있다() {
        UUID meetingId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();
        RecordingTokenIssuer tokenIssuer = new RecordingTokenIssuer();
        JoinMeetingUseCase useCase =
                new JoinMeetingUseCase(
                        new StubMeetingRepository(
                                Meeting.of(
                                        meetingId,
                                        "STT 장애 허용 회의",
                                        Instant.parse("2026-06-12T00:55:00Z"),
                                        Instant.parse("2026-06-12T01:55:00Z"),
                                        hostUserId,
                                        null,
                                        "LIVEKIT",
                                        "provider-room-stt-warning",
                                        MeetingStatus.SCHEDULED,
                                        null,
                                        null,
                                        "STT 장애 허용 테스트")),
                        tokenIssuer,
                        new FailingRealtimeSessionStarter(),
                        new MeetingGuestNameAllocator(),
                        FIXED_CLOCK);

        JoinMeetingResult result =
                useCase.execute(
                        new JoinMeetingCommand(
                                meetingId,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "게스트",
                                "guest-stt-warning-test"));

        assertEquals("provider-room-stt-warning", result.roomName());
        assertEquals("issued-token", result.token());
        assertTrue(tokenIssuer.lastCommand.participantIdentity().startsWith("user-"));
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

    private static final class RecordingRealtimeSessionStarter
            implements MeetingRealtimeSessionStarter {

        private boolean called;
        private UUID lastMeetingId;
        private UUID lastOrganizationId;
        private String lastRoomName;

        @Override
        public void ensureStarted(UUID meetingId, UUID organizationId, String roomName) {
            // 회의 입장 직전에 STT room 준비를 보장하는지만 검증하면 충분하다.
            this.called = true;
            this.lastMeetingId = meetingId;
            this.lastOrganizationId = organizationId;
            this.lastRoomName = roomName;
        }
    }

    private static final class FailingRealtimeSessionStarter
            implements MeetingRealtimeSessionStarter {

        @Override
        public void ensureStarted(UUID meetingId, UUID organizationId, String roomName) {
            throw new BusinessException(ErrorCode.STT_PROVIDER_UNAVAILABLE);
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
            return Optional.ofNullable(meeting)
                    .filter(savedMeeting -> savedMeeting.id().equals(id));
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
