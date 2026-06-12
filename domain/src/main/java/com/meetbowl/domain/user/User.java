package com.meetbowl.domain.user;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public class User {

    private final UUID id;
    private final String loginId;
    private final String passwordHash;
    private final String name;
    private final String email;
    private final UserRole role;
    private final UserStatus status;
    private final UUID affiliateId;
    private final UUID departmentId;
    private final UUID positionId;
    private final UUID teamId;
    private final boolean initialPasswordChangeRequired;
    private final Instant activeFrom;
    private final Instant activeUntil;
    private final Instant createdAt;
    private final Instant updatedAt;

    private User(
            UUID id,
            String loginId,
            String passwordHash,
            String name,
            String email,
            UserRole role,
            UserStatus status,
            UUID affiliateId,
            UUID departmentId,
            UUID positionId,
            UUID teamId,
            boolean initialPasswordChangeRequired,
            Instant activeFrom,
            Instant activeUntil,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
        this.affiliateId = affiliateId;
        this.departmentId = departmentId;
        this.positionId = positionId;
        this.teamId = teamId;
        this.initialPasswordChangeRequired = initialPasswordChangeRequired;
        this.activeFrom = activeFrom;
        this.activeUntil = activeUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User of(
            UUID id,
            String loginId,
            String passwordHash,
            String name,
            String email,
            UserRole role,
            UserStatus status,
            UUID affiliateId,
            UUID departmentId,
            UUID positionId,
            UUID teamId,
            boolean initialPasswordChangeRequired,
            Instant activeFrom,
            Instant activeUntil,
            Instant createdAt,
            Instant updatedAt) {
        validateRequired(loginId, "로그인 ID는 필수입니다.");
        validateRequired(passwordHash, "비밀번호 해시는 필수입니다.");
        validateRequired(name, "사용자 이름은 필수입니다.");
        validateRequired(email, "이메일은 필수입니다.");
        if (role == null || status == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "사용자 권한과 상태는 필수입니다.");
        }
        if (activeFrom != null && activeUntil != null && activeFrom.isAfter(activeUntil)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "활성 시작일은 종료일보다 이후일 수 없습니다.");
        }
        return new User(
                id,
                loginId,
                passwordHash,
                name,
                email,
                role,
                status,
                affiliateId,
                departmentId,
                positionId,
                teamId,
                initialPasswordChangeRequired,
                activeFrom,
                activeUntil,
                createdAt,
                updatedAt);
    }

    private static void validateRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
        }
    }

    public boolean isWithinActivePeriod(Instant now) {
        if (now == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "기준 시간은 필수입니다.");
        }
        boolean afterStart = activeFrom == null || !now.isBefore(activeFrom);
        boolean beforeEnd = activeUntil == null || !now.isAfter(activeUntil);
        return afterStart && beforeEnd;
    }

    public boolean isInactive() {
        return status == UserStatus.INACTIVE;
    }

    public boolean isLocked() {
        return status == UserStatus.LOCKED;
    }

    public boolean isExpired(Instant now) {
        return !isWithinActivePeriod(now);
    }

    public boolean canLoginAt(Instant now) {
        return status == UserStatus.ACTIVE && isWithinActivePeriod(now);
    }

    public boolean isSystemAccount() {
        return role == UserRole.SYSTEM;
    }

    public User completeInitialPasswordChange(String newPasswordHash) {
        validateRequired(newPasswordHash, "새 비밀번호 해시는 필수입니다.");
        if (!initialPasswordChangeRequired) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "초기 비밀번호 변경이 필요한 상태가 아닙니다.");
        }

        return new User(
                id,
                loginId,
                newPasswordHash,
                name,
                email,
                role,
                status,
                affiliateId,
                departmentId,
                positionId,
                teamId,
                false,
                activeFrom,
                activeUntil,
                createdAt,
                updatedAt);
    }

    public User resetPasswordByAdmin(String newPasswordHash) {
        validateRequired(newPasswordHash, "새 비밀번호 해시는 필수입니다.");

        return new User(
                id,
                loginId,
                newPasswordHash,
                name,
                email,
                role,
                status,
                affiliateId,
                departmentId,
                positionId,
                teamId,
                true,
                activeFrom,
                activeUntil,
                createdAt,
                updatedAt);
    }

    public User updateAdminProfile(
            String name,
            String email,
            UUID affiliateId,
            UUID departmentId,
            UUID positionId,
            UUID teamId,
            UserRole role,
            Instant activeFrom,
            Instant activeUntil) {
        validateRequired(name, "사용자 이름은 필수입니다.");
        validateRequired(email, "이메일은 필수입니다.");
        if (role == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "사용자 권한은 필수입니다.");
        }
        if (activeFrom != null && activeUntil != null && activeFrom.isAfter(activeUntil)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "활성 시작일은 종료일보다 이후일 수 없습니다.");
        }

        return new User(
                id,
                loginId,
                passwordHash,
                name,
                email,
                role,
                status,
                affiliateId,
                departmentId,
                positionId,
                teamId,
                initialPasswordChangeRequired,
                activeFrom,
                activeUntil,
                createdAt,
                updatedAt);
    }

    public User changeStatus(UserStatus newStatus) {
        if (newStatus == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "사용자 상태는 필수입니다.");
        }

        return new User(
                id,
                loginId,
                passwordHash,
                name,
                email,
                role,
                newStatus,
                affiliateId,
                departmentId,
                positionId,
                teamId,
                initialPasswordChangeRequired,
                activeFrom,
                activeUntil,
                createdAt,
                updatedAt);
    }

    public UUID id() {
        return id;
    }

    public String loginId() {
        return loginId;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public UserRole role() {
        return role;
    }

    public UserStatus status() {
        return status;
    }

    public UUID affiliateId() {
        return affiliateId;
    }

    public UUID departmentId() {
        return departmentId;
    }

    public UUID positionId() {
        return positionId;
    }

    public UUID teamId() {
        return teamId;
    }

    public boolean initialPasswordChangeRequired() {
        return initialPasswordChangeRequired;
    }

    public Instant activeFrom() {
        return activeFrom;
    }

    public Instant activeUntil() {
        return activeUntil;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
