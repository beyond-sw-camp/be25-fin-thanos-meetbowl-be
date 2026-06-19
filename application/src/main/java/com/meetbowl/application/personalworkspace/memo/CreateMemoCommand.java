package com.meetbowl.application.personalworkspace.memo;

import java.util.UUID;

/** 개인 메모 작성 UseCase 입력이다. 소유자/조직은 인증 사용자에서 채우고 제목·내용을 받는다. */
public record CreateMemoCommand(
        UUID ownerUserId, UUID organizationId, String title, String content) {}
