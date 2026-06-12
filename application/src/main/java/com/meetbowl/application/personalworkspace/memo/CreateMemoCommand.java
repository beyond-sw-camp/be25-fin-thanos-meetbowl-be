package com.meetbowl.application.personalworkspace.memo;

import java.util.UUID;

/** 개인 메모 작성 UseCase 입력이다. 소유자는 인증 사용자에서 채우고 제목·내용을 받는다. */
public record CreateMemoCommand(UUID ownerUserId, String title, String content) {}
