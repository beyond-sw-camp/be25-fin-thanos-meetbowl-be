package com.meetbowl.application.admin;

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

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    private final TransactionOperations transactionOperations;

    public ResetUserPasswordUseCase(
            UserRepositoryPort userRepositoryPort,
            PasswordEncoder passwordEncoder,
            TemporaryPasswordGenerator temporaryPasswordGenerator,
            AdminAuditLogRepositoryPort adminAuditLogRepositoryPort,
            TransactionOperations transactionOperations) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.adminAuditLogRepositoryPort = adminAuditLogRepositoryPort;
        this.transactionOperations = transactionOperations;
    }

    public ResetUserPasswordResult execute(ResetUserPasswordCommand command) {
        // 관리자 초기화는 1회용 임시 비밀번호를 생성하고 해시만 저장한다.
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

                            String temporaryPassword =
                                    temporaryPasswordGenerator.generateDistinctFrom(
                                            user.passwordHash(), passwordEncoder);
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
        // 감사 로그에는 관리자 작업 흔적만 남기고 원문 비밀번호는 남기지 않는다.
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
