package com.meetbowl.domain.minutes;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** 사용자가 개인 워크스페이스에서 다시 볼 회의록 즐겨찾기 항목이다. */
public class MinutesFavorite {

    private final UUID id;
    private final UUID userId;
    private final UUID minutesId;
    private final Instant favoritedAt;

    private MinutesFavorite(UUID id, UUID userId, UUID minutesId, Instant favoritedAt) {
        this.id = id;
        this.userId = userId;
        this.minutesId = minutesId;
        this.favoritedAt = favoritedAt;
    }

    public static MinutesFavorite create(UUID userId, UUID minutesId, Instant favoritedAt) {
        return of(null, userId, minutesId, favoritedAt);
    }

    public static MinutesFavorite of(UUID id, UUID userId, UUID minutesId, Instant favoritedAt) {
        if (userId == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "회의록 즐겨찾기 사용자 ID는 필수입니다.");
        }
        if (minutesId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의록 즐겨찾기 대상 ID는 필수입니다.");
        }
        if (favoritedAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의록 즐겨찾기 시각은 필수입니다.");
        }
        return new MinutesFavorite(id, userId, minutesId, favoritedAt);
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public UUID minutesId() {
        return minutesId;
    }

    public Instant favoritedAt() {
        return favoritedAt;
    }
}
