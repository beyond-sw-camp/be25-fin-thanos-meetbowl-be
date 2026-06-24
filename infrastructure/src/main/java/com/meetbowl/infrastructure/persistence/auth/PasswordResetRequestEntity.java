package com.meetbowl.infrastructure.persistence.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.meetbowl.domain.auth.PasswordResetRequest;
import com.meetbowl.domain.auth.PasswordResetRequestStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(name = "password_reset_requests")
public class PasswordResetRequestEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String requesterName;

    @Column(nullable = false, length = 100)
    private String loginId;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PasswordResetRequestStatus status;

    @Column(nullable = false)
    private Instant requestedAt;

    private Instant processedAt;

    @Column(columnDefinition = "BINARY(16)")
    private UUID processedByAdminId;

    protected PasswordResetRequestEntity() {}

    private PasswordResetRequestEntity(PasswordResetRequest request) {
        userId = request.userId();
        requesterName = request.requesterName();
        loginId = request.loginId();
        email = request.email();
        status = request.status();
        requestedAt = request.requestedAt();
        processedAt = request.processedAt();
        processedByAdminId = request.processedByAdminId();
    }

    static PasswordResetRequestEntity from(PasswordResetRequest request) {
        PasswordResetRequestEntity entity = new PasswordResetRequestEntity(request);
        entity.setId(request.id());
        return entity;
    }

    PasswordResetRequest toDomain() {
        return new PasswordResetRequest(
                getId(),
                userId,
                requesterName,
                loginId,
                email,
                status,
                requestedAt,
                processedAt,
                processedByAdminId,
                getCreatedAt(),
                getUpdatedAt());
    }
}
