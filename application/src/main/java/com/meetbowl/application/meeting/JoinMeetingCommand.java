package com.meetbowl.application.meeting;

import java.util.UUID;

/**
 * 회의 입장 정보 발급 요청이다.
 *
 * <p>현재 프론트는 아직 JWT 기반 로그인과 완전히 연결되지 않았기 때문에, 인증 사용자가 있으면 그 값을 우선 쓰고 없으면 화면에서 전달한
 * participantIdentity/displayName으로 LiveKit participant를 만든다. 이 record는 그 전환 상태를 UseCase 내부에서 일관되게
 * 다루기 위한 계약이다.
 */
public record JoinMeetingCommand(
        UUID meetingId,
        UUID authenticatedUserId,
        String displayName,
        String requestedParticipantIdentity) {}
