package com.meetbowl.infrastructure.persistence.video;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.video.GuestSession;
import com.meetbowl.domain.video.GuestSessionStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** 초대 코드 또는 URL로 발급된 게스트 회의 접근 세션을 저장하는 JPA Entity다. */
@Entity
@Table(
        name = "guest_sessions",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_guest_sessions_access_token_hash",
                    columnNames = "access_token_hash")
        },
        indexes = {
            @Index(name = "idx_guest_sessions_meeting_status", columnList = "meeting_id, status"),
            @Index(name = "idx_guest_sessions_expires_at", columnList = "expires_at")
        })
public class GuestSessionEntity extends BaseEntity {

    @Column(
            name = "meeting_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID meetingId;

    @Column(name = "invite_code_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID inviteCodeId;

    @Column(name = "guest_name", nullable = false, length = 100)
    private String guestName;

    @Column(name = "access_token_hash", nullable = false, updatable = false, length = 128)
    private String accessTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GuestSessionStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "last_ip", length = 45)
    private String lastIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    protected GuestSessionEntity() {}

    private GuestSessionEntity(
            UUID meetingId,
            UUID inviteCodeId,
            String guestName,
            String accessTokenHash,
            GuestSessionStatus status,
            Instant expiresAt,
            Instant joinedAt,
            Instant leftAt,
            String lastIp,
            String userAgent) {
        this.meetingId = meetingId;
        this.inviteCodeId = inviteCodeId;
        this.guestName = guestName;
        this.accessTokenHash = accessTokenHash;
        this.status = status;
        this.expiresAt = expiresAt;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
        this.lastIp = lastIp;
        this.userAgent = userAgent;
    }

    static GuestSessionEntity from(GuestSession guestSession) {
        GuestSessionEntity entity =
                new GuestSessionEntity(
                        guestSession.meetingId(),
                        guestSession.inviteCodeId(),
                        guestSession.guestName(),
                        guestSession.accessTokenHash(),
                        guestSession.status(),
                        guestSession.expiresAt(),
                        guestSession.joinedAt(),
                        guestSession.leftAt(),
                        guestSession.lastIp(),
                        guestSession.userAgent());
        entity.setId(guestSession.id());
        return entity;
    }

    GuestSession toDomain() {
        return GuestSession.of(
                getId(),
                meetingId,
                inviteCodeId,
                guestName,
                accessTokenHash,
                status,
                expiresAt,
                joinedAt,
                leftAt,
                lastIp,
                userAgent);
    }
}
