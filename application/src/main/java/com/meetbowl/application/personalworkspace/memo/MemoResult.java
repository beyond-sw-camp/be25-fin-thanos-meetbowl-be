package com.meetbowl.application.personalworkspace.memo;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemo;

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
