package com.meetbowl.domain.video;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** 초대 코드 또는 URL을 통해 발급된 게스트 회의 접근 세션이다. */
public class GuestSession {

    private final UUID id;
    private final UUID meetingId;
    private final UUID inviteCodeId;
    private final String guestName;
    private final String accessTokenHash;
    private final GuestSessionStatus status;
    private final Instant expiresAt;
    private final Instant joinedAt;
    private final Instant leftAt;
    private final String lastIp;
    private final String userAgent;

    private GuestSession(
            UUID id,
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
        this.id = id;
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

    public static GuestSession create(
            UUID meetingId,
            UUID inviteCodeId,
            String guestName,
            String accessTokenHash,
            Instant expiresAt,
            String lastIp,
            String userAgent) {
        return of(
                null,
                meetingId,
                inviteCodeId,
                guestName,
                accessTokenHash,
                GuestSessionStatus.ISSUED,
                expiresAt,
                null,
                null,
                lastIp,
                userAgent);
    }

    public static GuestSession of(
            UUID id,
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
        require(meetingId, "회의 ID는 필수입니다.");
        require(status, "게스트 세션 상태는 필수입니다.");
        require(expiresAt, "게스트 세션 만료 시각은 필수입니다.");
        if (guestName == null || guestName.isBlank()) {
            throw invalid("게스트 이름은 필수입니다.");
        }
        if (accessTokenHash == null || accessTokenHash.isBlank()) {
            throw invalid("게스트 접근 토큰 해시는 필수입니다.");
        }
        if (joinedAt != null && leftAt != null && leftAt.isBefore(joinedAt)) {
            throw invalid("게스트 퇴장 시각은 입장 시각보다 빠를 수 없습니다.");
        }
        return new GuestSession(
                id,
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

    private static void require(Object value, String message) {
        if (value == null) {
            throw invalid(message);
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
    }

    public UUID id() {
        return id;
    }

    public UUID meetingId() {
        return meetingId;
    }

    public UUID inviteCodeId() {
        return inviteCodeId;
    }

    public String guestName() {
        return guestName;
    }

    public String accessTokenHash() {
        return accessTokenHash;
    }

    public GuestSessionStatus status() {
        return status;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant joinedAt() {
        return joinedAt;
    }

    public Instant leftAt() {
        return leftAt;
    }

    public String lastIp() {
        return lastIp;
    }

    public String userAgent() {
        return userAgent;
    }
}
