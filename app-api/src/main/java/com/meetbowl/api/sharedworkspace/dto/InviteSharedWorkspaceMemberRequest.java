package com.meetbowl.api.sharedworkspace.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** 공유 워크스페이스 멤버 초대 요청 DTO다. 초대자는 인증 context에서 채우고 본문에는 초대 대상만 받는다. */
public record InviteSharedWorkspaceMemberRequest(
        @NotNull(message = "초대할 사용자 ID는 필수입니다.") UUID userId) {}
