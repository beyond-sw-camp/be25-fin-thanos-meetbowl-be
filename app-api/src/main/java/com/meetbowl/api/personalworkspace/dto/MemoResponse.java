package com.meetbowl.api.personalworkspace.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.personalworkspace.memo.MemoResult;

/** 개인 메모 응답 본문이다. 메모 결과를 클라이언트 계약으로 변환해 노출한다. */
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
