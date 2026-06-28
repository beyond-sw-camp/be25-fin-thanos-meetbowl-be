package com.meetbowl.application.admin;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.admin.AuditResult;
import com.meetbowl.domain.auth.TokenStateRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;

@Service
public class ResetUserPasswordUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    private final TransactionOperations transactionOperations;
    private final TokenStateRepositoryPort tokenStateRepositoryPort;
    private final ObjectMapper objectMapper;

    public ResetUserPasswordUseCase(
            UserRepositoryPort userRepositoryPort,
            PasswordEncoder passwordEncoder,
            AdminAuditLogRepositoryPort adminAuditLogRepositoryPort,
            TransactionOperations transactionOperations,
            TokenStateRepositoryPort tokenStateRepositoryPort,
            ObjectMapper objectMapper) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.adminAuditLogRepositoryPort = adminAuditLogRepositoryPort;
        this.transactionOperations = transactionOperations;
        this.tokenStateRepositoryPort = tokenStateRepositoryPort;
        this.objectMapper = objectMapper;
    }

    public ResetUserPasswordResult execute(ResetUserPasswordCommand command) {
        // 관리자 초기화는 1회용 임시 비밀번호를 생성하고 해시만 저장한다.
        ResetUserPasswordResult result =
                Objects.requireNonNull(
                        transactionOperations.execute(
                                status -> {
                                    User user =
                                            userRepositoryPort
                                                    .findById(command.userId())
                                                    .orElseThrow(
                                                            () ->
                                                                    new BusinessException(
                                                                            ErrorCode
                                                                                    .USER_NOT_FOUND));
                                    ensureManagedUser(user);
                                    ensureAccessibleToAdmin(user, command.adminAffiliateId());

                                    User updatedUser =
                                            user.resetPasswordByAdmin(
                                                    passwordEncoder.encode(
                                                            PasswordPolicy.INITIAL_PASSWORD));
                                    userRepositoryPort.save(updatedUser);

                                    logAudit(
                                            command,
                                            updatedUser.loginId(),
                                            updatedUser.name(),
                                            AuditResult.SUCCESS,
                                            null);
                                    return new ResetUserPasswordResult(
                                            PasswordPolicy.INITIAL_PASSWORD);
                                }));
        // 비밀번호 초기화 트랜잭션이 성공한 뒤 탈취 가능성이 있는 기존 세션을 모두 폐기한다.
        tokenStateRepositoryPort.revokeUserSessions(command.userId(), Instant.now());
        return result;
    }

    private void ensureManagedUser(User user) {
        // 초기 관리자와 시스템 계정의 자격 증명은 일반 회원 관리 흐름에서 변경하지 않는다.
        if (user.role() != UserRole.USER) {
            throw new BusinessException(
                    ErrorCode.COMMON_FORBIDDEN, "관리자 및 시스템 계정의 비밀번호는 초기화할 수 없습니다.");
        }
    }

    private void ensureAccessibleToAdmin(User user, UUID adminAffiliateId) {
        if (adminAffiliateId == null) {
            return;
        }
        if (!java.util.Objects.equals(user.affiliateId(), adminAffiliateId)) {
            throw new BusinessException(
                    ErrorCode.COMMON_FORBIDDEN, "다른 계열사의 회원은 관리할 수 없습니다.");
        }
    }

    private void logAudit(
            ResetUserPasswordCommand command,
            String targetLoginId,
            String targetName,
            AuditResult result,
            String failureReason) {
        // 감사 로그에는 관리자 작업 흔적만 남기고 원문 비밀번호는 남기지 않는다.
        AdminAuditLog log =
                new AdminAuditLog(
                        UUID.randomUUID(),
                        command.adminId(),
                        command.adminName(),
                        "USER",
                        command.userId(),
                        targetLoginId,
                        targetName,
                        "USER_MANAGEMENT",
                        "PASSWORD_RESET",
                        result,
                        null,
                        failureReason != null ? failureReason : passwordResetSnapshot(),
                        command.ipAddress(),
                        command.userAgent(),
                        Instant.now());
        adminAuditLogRepositoryPort.save(log);
    }

    private String passwordResetSnapshot() {
        try {
            // 감사 로그에는 임시 비밀번호나 해시를 남기지 않고, 초기 비밀번호 변경 필요 여부만 기록한다.
            return objectMapper.writeValueAsString(new PasswordResetAuditSnapshot(true));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_INTERNAL_ERROR, "Failed to serialize admin audit snapshot.");
        }
    }

    private record PasswordResetAuditSnapshot(boolean initialPasswordChangeRequired) {}
}
