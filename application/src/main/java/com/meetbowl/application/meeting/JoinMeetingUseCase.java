package com.meetbowl.application.meeting;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.LiveKitTokenIssueCommand;
import com.meetbowl.domain.meeting.LiveKitTokenIssueResult;
import com.meetbowl.domain.meeting.LiveKitTokenIssuer;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;

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

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final LiveKitTokenIssuer liveKitTokenIssuer;

    public JoinMeetingUseCase(
            MeetingRepositoryPort meetingRepositoryPort, LiveKitTokenIssuer liveKitTokenIssuer) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.liveKitTokenIssuer = liveKitTokenIssuer;
    }

    public JoinMeetingResult execute(JoinMeetingCommand command) {
        if (command.meetingId() == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의 ID는 필수입니다.");
        }

        String roomName = resolveRoomName(command.meetingId());
        String participantIdentity = resolveParticipantIdentity(command);
        String participantName = resolveParticipantName(command);

        LiveKitTokenIssueResult issuedToken =
                liveKitTokenIssuer.issue(
                        new LiveKitTokenIssueCommand(
                                roomName, participantIdentity, participantName));

        return new JoinMeetingResult(
                command.meetingId(),
                roomName,
                issuedToken.livekitUrl(),
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
    private String resolveRoomName(UUID meetingId) {
        return meetingRepositoryPort
                .findById(meetingId)
                .map(meeting -> meeting.providerRoomId())
                .filter(providerRoomId -> providerRoomId != null && !providerRoomId.isBlank())
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

    private String resolveParticipantName(JoinMeetingCommand command) {
        if (command.displayName() == null || command.displayName().isBlank()) {
            return "게스트";
        }
        return command.displayName().trim();
    }
}
