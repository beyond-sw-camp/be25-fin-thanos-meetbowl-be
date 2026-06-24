package com.meetbowl.application.admin;

import java.time.Instant;
import java.util.List;
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
import com.meetbowl.domain.auth.PasswordResetRequest;
import com.meetbowl.domain.auth.PasswordResetRequestRepositoryPort;
import com.meetbowl.domain.auth.PasswordResetRequestStatus;
import com.meetbowl.domain.auth.TokenStateRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;

@Service
public class AdminPasswordResetRequestUseCase {

    private final PasswordResetRequestRepositoryPort passwordResetRequestRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    private final TransactionOperations transactionOperations;
    private final TokenStateRepositoryPort tokenStateRepositoryPort;
    private final ObjectMapper objectMapper;

    public AdminPasswordResetRequestUseCase(
            PasswordResetRequestRepositoryPort passwordResetRequestRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            PasswordEncoder passwordEncoder,
            AdminAuditLogRepositoryPort adminAuditLogRepositoryPort,
            TransactionOperations transactionOperations,
            TokenStateRepositoryPort tokenStateRepositoryPort,
            ObjectMapper objectMapper) {
        this.passwordResetRequestRepositoryPort = passwordResetRequestRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.adminAuditLogRepositoryPort = adminAuditLogRepositoryPort;
        this.transactionOperations = transactionOperations;
        this.tokenStateRepositoryPort = tokenStateRepositoryPort;
        this.objectMapper = objectMapper;
    }

    public List<PasswordResetRequestResult> list(String status) {
        PasswordResetRequestStatus parsedStatus = parseStatus(status);
        List<PasswordResetRequest> requests =
                parsedStatus == null
                        ? passwordResetRequestRepositoryPort.findAll()
                        : passwordResetRequestRepositoryPort.findAllByStatus(parsedStatus);
        return requests.stream().map(PasswordResetRequestResult::from).toList();
    }

    public long countPending() {
        return passwordResetRequestRepositoryPort.countByStatus(PasswordResetRequestStatus.PENDING);
    }

    public PasswordResetRequestResult approve(DecisionCommand command) {
        ApprovalOutcome outcome =
                transactionOperations.execute(
                        status -> {
                            Instant now = Instant.now();
                            PasswordResetRequest request = getRequest(command.requestId());
                            PasswordResetRequest approvedRequest = request.approve(command.adminId(), now);
                            User user =
                                    userRepositoryPort
                                            .findById(request.userId())
                                            .orElseThrow(
                                                    () ->
                                                            new BusinessException(
                                                                    ErrorCode.USER_NOT_FOUND));
                            ensureManagedUser(user);

                            User updatedUser =
                                    user.resetPasswordByAdmin(
                                            passwordEncoder.encode(PasswordPolicy.INITIAL_PASSWORD));
                            userRepositoryPort.save(updatedUser);
                            passwordResetRequestRepositoryPort.save(approvedRequest);
                            logDecision(
                                    command,
                                    request,
                                    approvedRequest,
                                    "PASSWORD_RESET_REQUEST_APPROVE",
                                    now);
                            return new ApprovalOutcome(approvedRequest, updatedUser.id());
                        });

        if (outcome == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INTERNAL_ERROR,
                    "비밀번호 초기화 요청 승인 처리에 실패했습니다.");
        }

        tokenStateRepositoryPort.revokeUserSessions(outcome.userId(), Instant.now());
        return PasswordResetRequestResult.from(outcome.request());
    }

    public PasswordResetRequestResult reject(DecisionCommand command) {
        PasswordResetRequest request =
                transactionOperations.execute(
                        status -> {
                            Instant now = Instant.now();
                            PasswordResetRequest current = getRequest(command.requestId());
                            PasswordResetRequest rejectedRequest =
                                    current.reject(command.adminId(), now);
                            passwordResetRequestRepositoryPort.save(rejectedRequest);
                            logDecision(
                                    command,
                                    current,
                                    rejectedRequest,
                                    "PASSWORD_RESET_REQUEST_REJECT",
                                    now);
                            return rejectedRequest;
                        });
        if (request == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INTERNAL_ERROR,
                    "비밀번호 초기화 요청 거절 처리에 실패했습니다.");
        }
        return PasswordResetRequestResult.from(request);
    }

    private PasswordResetRequest getRequest(UUID requestId) {
        return passwordResetRequestRepositoryPort
                .findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSWORD_RESET_REQUEST_NOT_FOUND));
    }

    private PasswordResetRequestStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return PasswordResetRequestStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST,
                    "지원하지 않는 비밀번호 초기화 요청 상태입니다.");
        }
    }

    private void ensureManagedUser(User user) {
        if (user.role() != UserRole.USER) {
            throw new BusinessException(
                    ErrorCode.COMMON_FORBIDDEN,
                    "관리자 계정 비밀번호 초기화 요청은 승인할 수 없습니다.");
        }
    }

    private void logDecision(
            DecisionCommand command,
            PasswordResetRequest before,
            PasswordResetRequest after,
            String actionName,
            Instant occurredAt) {
        adminAuditLogRepositoryPort.save(
                new AdminAuditLog(
                        UUID.randomUUID(),
                        command.adminId(),
                        command.adminName(),
                        "USER",
                        before.userId(),
                        before.loginId(),
                        before.requesterName(),
                        "AUTH",
                        actionName,
                        AuditResult.SUCCESS,
                        serializeSnapshot(before),
                        serializeSnapshot(after),
                        command.ipAddress(),
                        command.userAgent(),
                        occurredAt));
    }

    private String serializeSnapshot(PasswordResetRequest request) {
        try {
            return objectMapper.writeValueAsString(
                    new PasswordResetRequestAuditSnapshot(
                            request.status().name(), request.requestedAt(), request.processedAt()));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_INTERNAL_ERROR,
                    "비밀번호 초기화 요청 감사 로그 직렬화에 실패했습니다.");
        }
    }

    public record DecisionCommand(
            UUID requestId, UUID adminId, String adminName, String ipAddress, String userAgent) {}

    private record ApprovalOutcome(PasswordResetRequest request, UUID userId) {}

    private record PasswordResetRequestAuditSnapshot(
            String status, Instant requestedAt, Instant processedAt) {}
}
