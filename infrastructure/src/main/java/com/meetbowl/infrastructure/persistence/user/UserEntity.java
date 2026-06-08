package com.meetbowl.infrastructure.persistence.user;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String loginId;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(columnDefinition = "BINARY(16)")
    private UUID organizationId;

    @Column(columnDefinition = "BINARY(16)")
    private UUID departmentId;

    @Column(columnDefinition = "BINARY(16)")
    private UUID positionId;

    @Column(columnDefinition = "BINARY(16)")
    private UUID jobId;

    @Column(nullable = false)
    private boolean initialPasswordChangeRequired;

    private Instant activeFrom;

    private Instant activeUntil;

    protected UserEntity() {}

    private UserEntity(User user) {
        this.loginId = user.loginId();
        this.passwordHash = user.passwordHash();
        this.name = user.name();
        this.email = user.email();
        this.role = user.role();
        this.status = user.status();
        this.organizationId = user.organizationId();
        this.departmentId = user.departmentId();
        this.positionId = user.positionId();
        this.jobId = user.jobId();
        this.initialPasswordChangeRequired = user.initialPasswordChangeRequired();
        this.activeFrom = user.activeFrom();
        this.activeUntil = user.activeUntil();
    }

    static UserEntity from(User user) {
        UserEntity entity = new UserEntity(user);
        entity.setId(user.id());
        return entity;
    }

    User toDomain() {
        return User.of(
                getId(),
                loginId,
                passwordHash,
                name,
                email,
                role,
                status,
                organizationId,
                departmentId,
                positionId,
                jobId,
                initialPasswordChangeRequired,
                activeFrom,
                activeUntil,
                getCreatedAt(),
                getUpdatedAt());
    }
}
