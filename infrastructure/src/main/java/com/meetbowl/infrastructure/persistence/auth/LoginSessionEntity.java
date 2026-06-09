package com.meetbowl.infrastructure.persistence.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.meetbowl.domain.auth.LoginSession;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 사용자 로그인 세션을 저장하는 엔티티다.
 * 세션 토큰, 만료 시각, 최근 로그인 정보 등을 보관한다.
 */
@Entity
@Table(name = "login_sessions")
public class LoginSessionEntity extends BaseEntity {
    /** 세션 소유자 사용자 ID(UUID). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    /** 세션 토큰 식별자(고유). */
    @Column(nullable = false, unique = true, length = 255)
    private String sessionTokenId;

    /** 세션 활성 여부. */
    @Column(nullable = false)
    private boolean active;

    /** 세션 만료 시각(UTC). */
    @Column(nullable = false)
    private Instant expiresAt;

    /** 최근 로그인 시각(UTC). */
    @Column(nullable = false)
    private Instant lastLoginAt;

    /** 로그인 요청 IP 주소. */
    @Column(length = 100)
    private String ipAddress;

    /** 로그인 요청 User-Agent. */
    @Column(length = 500)
    private String userAgent;

    protected LoginSessionEntity() {}

    private LoginSessionEntity(LoginSession session) {
        userId = session.userId();
        sessionTokenId = session.sessionTokenId();
        active = session.active();
        expiresAt = session.expiresAt();
        lastLoginAt = session.lastLoginAt();
        ipAddress = session.ipAddress();
        userAgent = session.userAgent();
    }

    static LoginSessionEntity from(LoginSession session) {
        LoginSessionEntity entity = new LoginSessionEntity(session);
        entity.setId(session.id());
        return entity;
    }

    LoginSession toDomain() {
        return new LoginSession(
                getId(),
                userId,
                sessionTokenId,
                active,
                expiresAt,
                lastLoginAt,
                ipAddress,
                userAgent,
                getCreatedAt(),
                getUpdatedAt());
    }
}
