package com.meetbowl.api.meeting.dto;

import jakarta.validation.constraints.Size;

/**
 * 회의 입장용 participant 정보를 받는다.
 *
 * <p>participantIdentity는 선택값이다. 로그인 사용자가 있으면 서버가 user-{uuid} 규칙으로 덮어쓰고, 로그인 연동 전 화면에서는 전달값을 그대로
 * 사용한다.
 */
public record JoinMeetingRequest(
        @Size(max = 100, message = "displayName은 100자 이하여야 합니다.")
                String displayName,
        @Size(max = 120, message = "participantIdentity는 120자 이하여야 합니다.")
                String participantIdentity) {}
