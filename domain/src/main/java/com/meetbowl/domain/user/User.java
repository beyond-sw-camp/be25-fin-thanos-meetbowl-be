package com.meetbowl.domain.user;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
    private final Instant deletedAt;
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
            Instant deletedAt,
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
        this.deletedAt = deletedAt;
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
        return of(
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
                null,
                createdAt,
                updatedAt);
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
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt) {
        validateRequired(loginId, "濡쒓렇??ID???꾩닔?낅땲??");
        validateRequired(passwordHash, "鍮꾨?踰덊샇 ?댁떆???꾩닔?낅땲??");
        validateRequired(name, "?ъ슜???대쫫? ?꾩닔?낅땲??");
        validateRequired(email, "?대찓?쇱? ?꾩닔?낅땲??");
        if (role == null || status == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "?ъ슜??沅뚰븳怨??곹깭???꾩닔?낅땲??");
        }
        if (activeFrom != null && activeUntil != null && activeFrom.isAfter(activeUntil)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "?쒖꽦 ?쒖옉?쇱? 醫낅즺?쇰낫???댄썑?????놁뒿?덈떎.");
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
                deletedAt,
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
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "湲곗? ?쒓컙? ?꾩닔?낅땲??");
        }
        Instant dayStart = toUtcDate(now).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant nextDayStart = dayStart.plusSeconds(24 * 60 * 60);
        boolean afterStart = activeFrom == null || activeFrom.isBefore(nextDayStart);
        boolean beforeEnd = activeUntil == null || !activeUntil.isBefore(dayStart);
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

    public UserStatus effectiveStatusAt(Instant now) {
        if (isDeleted()) {
            return UserStatus.INACTIVE;
        }
        if (status == UserStatus.LOCKED) {
            return UserStatus.LOCKED;
        }
        if (status != UserStatus.ACTIVE) {
            return status;
        }
        return isWithinActivePeriod(now) ? UserStatus.ACTIVE : UserStatus.INACTIVE;
    }

    public boolean canLoginAt(Instant now) {
        return !isDeleted() && effectiveStatusAt(now) == UserStatus.ACTIVE;
    }

    public boolean isSystemAccount() {
        return role == UserRole.SYSTEM;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public User completeInitialPasswordChange(String newPasswordHash) {
        validateRequired(newPasswordHash, "??鍮꾨?踰덊샇 ?댁떆???꾩닔?낅땲??");
        if (!initialPasswordChangeRequired) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "珥덇린 鍮꾨?踰덊샇 蹂寃쎌씠 ?꾩슂???곹깭媛 ?꾨떃?덈떎.");
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
                deletedAt,
                createdAt,
                updatedAt);
    }

    public User resetPasswordByAdmin(String newPasswordHash) {
        validateRequired(newPasswordHash, "??鍮꾨?踰덊샇 ?댁떆???꾩닔?낅땲??");

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
                deletedAt,
                createdAt,
                updatedAt);
    }

    public User changePassword(String newPasswordHash) {
        validateRequired(newPasswordHash, "??鍮꾨?踰덊샇 ?댁떆???꾩닔?낅땲??");

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
                deletedAt,
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
        validateRequired(name, "?ъ슜???대쫫? ?꾩닔?낅땲??");
        validateRequired(email, "?대찓?쇱? ?꾩닔?낅땲??");
        if (role == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "?ъ슜??沅뚰븳? ?꾩닔?낅땲??");
        }
        if (activeFrom != null && activeUntil != null && activeFrom.isAfter(activeUntil)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "?쒖꽦 ?쒖옉?쇱? 醫낅즺?쇰낫???댄썑?????놁뒿?덈떎.");
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
                deletedAt,
                createdAt,
                updatedAt);
    }

    public User updateMyProfile(String name, String email) {
        validateRequired(name, "User name is required.");
        validateRequired(email, "Email is required.");

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
                deletedAt,
                createdAt,
                updatedAt);
    }

    public User changeStatus(UserStatus newStatus) {
        if (newStatus == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "?ъ슜???곹깭???꾩닔?낅땲??");
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
                deletedAt,
                createdAt,
                updatedAt);
    }

    public User delete(Instant deletedAt, String deletedLoginId, String deletedEmail) {
        if (deletedAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "??젣 ?쒓컙? ?꾩닔?낅땲??");
        }
        validateRequired(deletedLoginId, "??젣???濡쒓렇??ID??蹂댁젙媛믪씠 ?꾩닔?낅땲??");
        validateRequired(deletedEmail, "??젣???대찓???댁젙媛믪씠 ?꾩닔?낅땲??");

        return new User(
                id,
                deletedLoginId,
                passwordHash,
                name,
                deletedEmail,
                role,
                status,
                affiliateId,
                departmentId,
                positionId,
                teamId,
                initialPasswordChangeRequired,
                activeFrom,
                activeUntil,
                deletedAt,
                createdAt,
                updatedAt);
    }

    private LocalDate toUtcDate(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
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

    public Instant deletedAt() {
        return deletedAt;
    }
}
