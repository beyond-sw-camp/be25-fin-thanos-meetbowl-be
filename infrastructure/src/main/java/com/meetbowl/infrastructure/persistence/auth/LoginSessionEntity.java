package com.meetbowl.infrastructure.persistence.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.meetbowl.domain.auth.LoginSession;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(name = "login_sessions")
public class LoginSessionEntity extends BaseEntity {
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(nullable = false, unique = true, length = 255)
    private String sessionTokenId;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant lastLoginAt;

    @Column(length = 100)
    private String ipAddress;

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
