package com.meetbowl.application.personalworkspace.memo;

import java.util.UUID;

/** 개인 메모 수정 UseCase의 입력 모델이다. ownerUserId로 본인 메모만 수정하도록 조회를 제한한다. */
public record UpdateMemoCommand(UUID memoId, UUID ownerUserId, String title, String content) {}
