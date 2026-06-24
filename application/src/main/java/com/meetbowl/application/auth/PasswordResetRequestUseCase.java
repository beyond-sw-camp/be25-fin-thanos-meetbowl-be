package com.meetbowl.application.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.admin.AuditResult;
import com.meetbowl.domain.auth.PasswordResetRequest;
import com.meetbowl.domain.auth.PasswordResetRequestRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserRepositoryPort;

@Service
public class PasswordResetRequestUseCase {

    public static final String ACCEPTED_MESSAGE = "비밀번호 재설정 요청이 접수되었습니다. 관리자 확인 후 처리됩니다.";

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordResetRequestRepositoryPort passwordResetRequestRepositoryPort;
    private final AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    private final ObjectMapper objectMapper;

    public PasswordResetRequestUseCase(
            UserRepositoryPort userRepositoryPort,
            PasswordResetRequestRepositoryPort passwordResetRequestRepositoryPort,
            AdminAuditLogRepositoryPort adminAuditLogRepositoryPort,
            ObjectMapper objectMapper) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordResetRequestRepositoryPort = passwordResetRequestRepositoryPort;
        this.adminAuditLogRepositoryPort = adminAuditLogRepositoryPort;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void execute(PasswordResetRequestCommand command) {
        String normalizedLoginId = normalizeRequired(command.loginId(), "loginId");
        String normalizedEmail = normalizeRequired(command.email(), "email");

        // 계정 존재 여부를 응답으로 노출하면 loginId/email 추측 시도가 쉬워지므로
        // 일치하는 계정이 있을 때만 감사 로그를 남기고 외부 응답은 항상 동일하게 유지한다.
        userRepositoryPort
                .findByLoginId(normalizedLoginId)
                .filter(user -> isEmailMatched(user, normalizedEmail))
                .filter(user -> user.role() == UserRole.USER)
                .ifPresent(user -> saveRequest(user, command.ipAddress(), command.userAgent()));
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, fieldName + " is required.");
        }
        return value.trim();
    }

    private boolean isEmailMatched(User user, String normalizedEmail) {
        return user.email() != null && user.email().trim().equalsIgnoreCase(normalizedEmail);
    }

    private void saveRequest(User user, String ipAddress, String userAgent) {
        Instant requestedAt = Instant.now();
        passwordResetRequestRepositoryPort.save(
                PasswordResetRequest.create(
                        UUID.randomUUID(),
                        user.id(),
                        user.name(),
                        user.loginId(),
                        user.email(),
                        requestedAt));

        adminAuditLogRepositoryPort.save(
                new AdminAuditLog(
                        UUID.randomUUID(),
                        user.id(),
                        user.name(),
                        "USER",
                        user.id(),
                        user.loginId(),
                        user.name(),
                        "AUTH",
                        "PASSWORD_RESET_REQUEST",
                        AuditResult.SUCCESS,
                        null,
                        snapshot(),
                        ipAddress,
                        userAgent,
                        requestedAt));
    }

    private String snapshot() {
        try {
            return objectMapper.writeValueAsString(
                    new PasswordResetRequestAuditSnapshot("PUBLIC_API"));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_INTERNAL_ERROR,
                    "Failed to serialize password reset request audit snapshot.");
        }
    }

    private record PasswordResetRequestAuditSnapshot(String requestSource) {}
}
