package com.meetbowl.application.admin;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.admin.AuditResult;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

@Service
public class ResetUserPasswordUseCase {

    private static final int MINIMUM_PASSWORD_LENGTH = 8;
    private static final int MAXIMUM_PASSWORD_LENGTH = 100;

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    private final TransactionOperations transactionOperations;

    public ResetUserPasswordUseCase(
            UserRepositoryPort userRepositoryPort,
            PasswordEncoder passwordEncoder,
            AdminAuditLogRepositoryPort adminAuditLogRepositoryPort,
            TransactionOperations transactionOperations) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.adminAuditLogRepositoryPort = adminAuditLogRepositoryPort;
        this.transactionOperations = transactionOperations;
    }

    public void execute(ResetUserPasswordCommand command) {
        validatePassword(command.newPassword());

        transactionOperations.executeWithoutResult(
                status -> {
                    User user =
                            userRepositoryPort
                                    .findById(command.userId())
                                    .orElseThrow(
                                            () -> new BusinessException(ErrorCode.USER_NOT_FOUND));

                    User updatedUser =
                            user.resetPasswordByAdmin(
                                    passwordEncoder.encode(command.newPassword()));
                    userRepositoryPort.save(updatedUser);

                    logAudit(command, AuditResult.SUCCESS, null);
                });
    }

    private void validatePassword(String password) {
        if (password == null
                || password.length() < MINIMUM_PASSWORD_LENGTH
                || password.length() > MAXIMUM_PASSWORD_LENGTH) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "비밀번호는 8자 이상 100자 이하여야 합니다.");
        }
    }

    private void logAudit(
            ResetUserPasswordCommand command, AuditResult result, String failureReason) {
        AdminAuditLog log =
                new AdminAuditLog(
                        UUID.randomUUID(),
                        command.adminId(),
                        command.adminName(),
                        "USER",
                        command.userId(),
                        "USER_MANAGEMENT",
                        "PASSWORD_RESET",
                        result,
                        null,
                        failureReason != null ? failureReason : "PASSWORD_RESET_COMPLETED",
                        command.ipAddress(),
                        command.userAgent(),
                        Instant.now());
        adminAuditLogRepositoryPort.save(log);
    }
}
