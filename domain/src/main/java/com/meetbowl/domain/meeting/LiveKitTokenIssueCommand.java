package com.meetbowl.domain.meeting;

/**
 * LiveKit JWT 생성에 필요한 최소 입력값이다.
 *
 * <p>회의 입장 API는 roomName/participantIdentity/displayName 조합만 정해 주고, 실제 JWT claim 구조와 서명 책임은
 * Infrastructure adapter로 넘긴다.
 */
public record LiveKitTokenIssueCommand(
        String roomName, String participantIdentity, String participantName) {}
