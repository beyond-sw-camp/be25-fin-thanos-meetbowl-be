package com.meetbowl.api.personalworkspace.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.personalworkspace.memo.MemoResult;

public record MemoResponse(
        UUID memoId,
        UUID ownerUserId,
        String title,
        String content,
        Instant createdAt,
        Instant updatedAt) {

    public static MemoResponse from(MemoResult result) {
        return new MemoResponse(
                result.memoId(),
                result.ownerUserId(),
                result.title(),
                result.content(),
                result.createdAt(),
                result.updatedAt());
    }
}
