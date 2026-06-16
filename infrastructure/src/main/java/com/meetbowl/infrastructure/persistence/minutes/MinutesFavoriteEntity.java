package com.meetbowl.infrastructure.persistence.minutes;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.minutes.MinutesFavorite;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** 사용자가 개인 워크스페이스에서 다시 볼 회의록 즐겨찾기 항목을 저장한다. */
@Entity
@Table(
        name = "minutes_favorites",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_minutes_favorites_user_minutes",
                        columnNames = {"user_id", "minutes_id"}))
public class MinutesFavoriteEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "minutes_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID minutesId;

    @Column(nullable = false)
    private Instant favoritedAt;

    protected MinutesFavoriteEntity() {}

    private MinutesFavoriteEntity(UUID userId, UUID minutesId, Instant favoritedAt) {
        this.userId = userId;
        this.minutesId = minutesId;
        this.favoritedAt = favoritedAt;
    }

    static MinutesFavoriteEntity from(MinutesFavorite favorite) {
        MinutesFavoriteEntity entity =
                new MinutesFavoriteEntity(
                        favorite.userId(), favorite.minutesId(), favorite.favoritedAt());
        entity.setId(favorite.id());
        return entity;
    }

    MinutesFavorite toDomain() {
        return MinutesFavorite.of(getId(), userId, minutesId, favoritedAt);
    }
}
