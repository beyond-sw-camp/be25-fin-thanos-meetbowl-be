package com.meetbowl.domain.sampletask;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** 계층 구조 예시를 위한 샘플 도메인 모델이다. 실제 업무 기능이 아니므로 운영 코드에서 참조하지 않는다. */
public class SampleTask {

    private final UUID id;
    private final UUID ownerUserId;
    private final String title;
    private final Instant createdAt;

    private SampleTask(UUID id, UUID ownerUserId, String title, Instant createdAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.createdAt = createdAt;
    }

    public static SampleTask create(UUID ownerUserId, String title, Instant createdAt) {
        return of(null, ownerUserId, title, createdAt);
    }

    public static SampleTask of(UUID id, UUID ownerUserId, String title, Instant createdAt) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "샘플 작업 제목은 필수입니다.");
        }
        return new SampleTask(id, ownerUserId, title, createdAt);
    }

    public UUID id() {
        return id;
    }

    public UUID ownerUserId() {
        return ownerUserId;
    }

    public String title() {
        return title;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
