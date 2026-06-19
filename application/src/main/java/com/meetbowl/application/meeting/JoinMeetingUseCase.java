package com.meetbowl.application.meeting;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.LiveKitTokenIssueCommand;
import com.meetbowl.domain.meeting.LiveKitTokenIssueResult;
import com.meetbowl.domain.meeting.LiveKitTokenIssuer;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.meeting.MeetingStatus;

/**
 * 회의 입장용 LiveKit 접속 정보를 발급한다.
 *
 * <p>정상적인 운영 환경에서는 meeting.providerRoomId를 사용해 기존 회의 room에 붙고, 아직 회의 생성/조회가 완전히 붙지 않은 개발 환경에서는
 * meeting-{meetingId} 규칙으로 fallback room을 만든다. 이렇게 하면 토큰 발급 책임은 BE로 옮기면서도, 프론트 mock 화면이 바로 깨지지는
 * 않는다.
 */
@Service
@Transactional(readOnly = true)
public class JoinMeetingUseCase {

    private static final String DEFAULT_GUEST_PARTICIPANT_NAME_PREFIX = "게스트";
    private static final String DEFAULT_GUEST_DISPLAY_NAME = "게스트";
    private static final Duration JOIN_AVAILABLE_BEFORE = Duration.ofMinutes(15);

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final LiveKitTokenIssuer liveKitTokenIssuer;
    private final MeetingRealtimeSessionStarter meetingRealtimeSessionStarter;
    private final MeetingGuestNameAllocator meetingGuestNameAllocator;
    private final Clock clock;

    public JoinMeetingUseCase(
            MeetingRepositoryPort meetingRepositoryPort,
            LiveKitTokenIssuer liveKitTokenIssuer,
            MeetingRealtimeSessionStarter meetingRealtimeSessionStarter,
            MeetingGuestNameAllocator meetingGuestNameAllocator,
            Clock clock) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.liveKitTokenIssuer = liveKitTokenIssuer;
        this.meetingRealtimeSessionStarter = meetingRealtimeSessionStarter;
        this.meetingGuestNameAllocator = meetingGuestNameAllocator;
        this.clock = clock;
    }

    public JoinMeetingResult execute(JoinMeetingCommand command) {
        if (command.meetingId() == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의 ID는 필수입니다.");
        }

        Meeting meeting = meetingRepositoryPort.findById(command.meetingId()).orElse(null);
        if (meeting != null && meeting.status() == MeetingStatus.ENDED) {
            throw new BusinessException(ErrorCode.MEETING_ALREADY_ENDED);
        }
        validateJoinWindow(meeting);
        String roomName = resolveRoomName(meeting, command.meetingId());
        String participantIdentity = resolveParticipantIdentity(command);
        String participantName = resolveParticipantName(command);

        // 사용자가 회의 화면에 입장하는 시점에 STT 세션도 같이 보장해야, 자막 탭이 빈 상태로 오래 머무르지 않는다.
        meetingRealtimeSessionStarter.ensureStarted(command.meetingId(), roomName);

        LiveKitTokenIssueResult issuedToken =
                liveKitTokenIssuer.issue(
                        new LiveKitTokenIssueCommand(
                                roomName, participantIdentity, participantName));

        return new JoinMeetingResult(
                command.meetingId(),
                roomName,
                issuedToken.livekitUrl(),
                meeting != null ? meeting.hostUserId() : null,
                participantIdentity,
                participantName,
                issuedToken.token(),
                issuedToken.issuedAt(),
                issuedToken.expiresAt());
    }

    /**
     * providerRoomId가 이미 저장돼 있으면 그 값을 신뢰한다.
     *
     * <p>다만 현재 화면은 mock meetingId로도 회의 입장 테스트를 해야 하므로, DB에 회의가 없거나 providerRoomId가 비어 있더라도 즉시 404를
     * 내지 않고 deterministic fallback room을 만든다.
     */
    private String resolveRoomName(Meeting meeting, UUID meetingId) {
        return meeting == null
                ? "meeting-" + meetingId
                : java.util.Optional.ofNullable(meeting.providerRoomId())
                        .filter(providerRoomId -> !providerRoomId.isBlank())
                        .orElse("meeting-" + meetingId);
    }

    /**
     * 인증 사용자가 있으면 그 사용자를 LiveKit participant identity의 기준으로 삼는다.
     *
     * <p>인증이 아직 붙지 않은 화면에서도 동일 API를 재사용해야 하므로, 요청값이 있으면 그 값을 쓰고 둘 다 없으면 임시 guest identity를 만든다.
     */
    private String resolveParticipantIdentity(JoinMeetingCommand command) {
        if (command.authenticatedUserId() != null) {
            return "user-" + command.authenticatedUserId();
        }
        if (command.requestedParticipantIdentity() != null
                && !command.requestedParticipantIdentity().isBlank()) {
            return command.requestedParticipantIdentity().trim();
        }
        return "guest-" + UUID.randomUUID();
    }

    /**
     * 게스트는 화면에 같은 이름으로만 보이면 누가 누구인지 구분이 안 되므로, 서버에서 구분 가능한 번호를 붙인다.
     *
     * <p>비로그인 게스트는 입력한 이름이 비어 있거나 기본값인 경우만 "게스트 {번호}"로 만든다. 번호는 participantIdentity를 기반으로
     * 계산해 같은 입장 흐름에서는 일관되게 유지되도록 한다.
     */
    private String resolveParticipantName(JoinMeetingCommand command) {
        String requestedDisplayName = normalizeDisplayName(command.displayName());
        if (command.authenticatedUserId() != null) {
            return requestedDisplayName.isBlank() ? DEFAULT_GUEST_DISPLAY_NAME : requestedDisplayName;
        }
        if (!isDefaultGuestDisplayName(requestedDisplayName)) {
            return requestedDisplayName;
        }
        return DEFAULT_GUEST_PARTICIPANT_NAME_PREFIX
                + " "
                + meetingGuestNameAllocator.nextGuestSequence(command.meetingId());
    }

    private String normalizeDisplayName(String displayName) {
        return displayName == null ? "" : displayName.trim();
    }

    private boolean isDefaultGuestDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return true;
        }
        return DEFAULT_GUEST_DISPLAY_NAME.equals(displayName)
                || "참석자".equals(displayName);
    }

    /**
     * 예약 회의는 로비를 너무 일찍 열어두면 실사용자 혼선을 키우므로, 예정 시작 15분 전부터만 입장을 허용한다.
     *
     * <p>회의가 DB에 없으면 로컬 개발용 fallback room 경로로 간주해 기존처럼 즉시 입장을 허용한다.
     */
    private void validateJoinWindow(Meeting meeting) {
        if (meeting == null) {
            return;
        }

        Instant joinAvailableAt = meeting.scheduledAt().minus(JOIN_AVAILABLE_BEFORE);
        if (Instant.now(clock).isBefore(joinAvailableAt)) {
            throw new BusinessException(ErrorCode.MEETING_JOIN_TOO_EARLY);
        }
    }

}
