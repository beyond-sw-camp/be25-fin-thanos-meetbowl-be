package com.meetbowl.application.personalworkspace.memo;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemo;

/** 개인 메모 결과다. 메모 식별자·소유자와 제목/내용, 생성·수정 시각을 담는다. */
public record MemoResult(
        UUID memoId,
        UUID ownerUserId,
        String title,
        String content,
        Instant createdAt,
        Instant updatedAt) {

    public static MemoResult from(PersonalWorkspaceMemo memo) {
        return new MemoResult(
                memo.id(),
                memo.ownerUserId(),
                memo.title(),
                memo.content(),
                memo.memoCreatedAt(),
                memo.memoUpdatedAt());
    }
}
