package com.meetbowl.application.admin;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
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

    private static final int TEMPORARY_PASSWORD_LENGTH = 16;
    private static final char[] TEMPORARY_PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    private final TransactionOperations transactionOperations;
    private final SecureRandom secureRandom = new SecureRandom();

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

    public ResetUserPasswordResult execute(ResetUserPasswordCommand command) {
        return Objects.requireNonNull(
                transactionOperations.execute(
                        status -> {
                            User user =
                                    userRepositoryPort
                                            .findById(command.userId())
                                            .orElseThrow(
                                                    () ->
                                                            new BusinessException(
                                                                    ErrorCode.USER_NOT_FOUND));

                            String temporaryPassword = generateTemporaryPassword(user);
                            User updatedUser =
                                    user.resetPasswordByAdmin(
                                            passwordEncoder.encode(temporaryPassword));
                            userRepositoryPort.save(updatedUser);

                            logAudit(command, AuditResult.SUCCESS, null);
                            return new ResetUserPasswordResult(temporaryPassword);
                        }));
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

    private String generateTemporaryPassword(User user) {
        for (int attempt = 0; attempt < 10; attempt++) {
            String temporaryPassword = generateRandomPassword();
            if (!passwordEncoder.matches(temporaryPassword, user.passwordHash())) {
                return temporaryPassword;
            }
        }

        throw new BusinessException(
                ErrorCode.COMMON_INTERNAL_ERROR, "임시 비밀번호를 생성할 수 없습니다.");
    }

    private String generateRandomPassword() {
        StringBuilder builder = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
        for (int index = 0; index < TEMPORARY_PASSWORD_LENGTH; index++) {
            int randomIndex = secureRandom.nextInt(TEMPORARY_PASSWORD_ALPHABET.length);
            builder.append(TEMPORARY_PASSWORD_ALPHABET[randomIndex]);
        }
        return builder.toString();
    }
}
