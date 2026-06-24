package com.meetbowl.application.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.auth.PasswordResetRequest;
import com.meetbowl.domain.auth.PasswordResetRequestRepositoryPort;
import com.meetbowl.domain.auth.PasswordResetRequestStatus;
import com.meetbowl.domain.auth.TokenStateRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class AdminPasswordResetRequestUseCaseTest {

    @Mock private PasswordResetRequestRepositoryPort passwordResetRequestRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    @Mock private TransactionOperations transactionOperations;
    @Mock private TokenStateRepositoryPort tokenStateRepositoryPort;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void listFiltersByStatus() {
        PasswordResetRequest pending = request(PasswordResetRequestStatus.PENDING);
        given(passwordResetRequestRepositoryPort.findAllByStatus(PasswordResetRequestStatus.PENDING))
                .willReturn(List.of(pending));

        List<PasswordResetRequestResult> results = useCase().list("PENDING");

        assertEquals(1, results.size());
        assertEquals("PENDING", results.get(0).status());
    }

    @Test
    void approveResetsPasswordAndMarksRequestApproved() {
        PasswordResetRequest request = request(PasswordResetRequestStatus.PENDING);
        User user = user(request.userId(), UserRole.USER);
        given(passwordResetRequestRepositoryPort.findById(request.id())).willReturn(Optional.of(request));
        given(userRepositoryPort.findById(request.userId())).willReturn(Optional.of(user));
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(passwordResetRequestRepositoryPort.save(any()))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(adminAuditLogRepositoryPort.save(any()))
                .willAnswer(invocation -> invocation.getArgument(0));
        executeTransactionCallback();

        PasswordResetRequestResult result =
                useCase()
                        .approve(
                                new AdminPasswordResetRequestUseCase.DecisionCommand(
                                        request.id(),
                                        UUID.randomUUID(),
                                        "admin",
                                        "127.0.0.1",
                                        "JUnit"));

        assertEquals("APPROVED", result.status());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(userCaptor.capture());
        verify(tokenStateRepositoryPort).revokeUserSessions(any(), any());
        ArgumentCaptor<AdminAuditLog> logCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepositoryPort).save(logCaptor.capture());
        assertEquals("PASSWORD_RESET_REQUEST_APPROVE", logCaptor.getValue().actionName());
    }

    @Test
    void rejectFailsWhenAlreadyProcessed() {
        PasswordResetRequest request = request(PasswordResetRequestStatus.REJECTED);
        given(passwordResetRequestRepositoryPort.findById(request.id())).willReturn(Optional.of(request));
        executeTransactionCallback();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase()
                                        .reject(
                                                new AdminPasswordResetRequestUseCase.DecisionCommand(
                                                        request.id(),
                                                        UUID.randomUUID(),
                                                        "admin",
                                                        "127.0.0.1",
                                                        "JUnit")));

        assertEquals(ErrorCode.PASSWORD_RESET_REQUEST_ALREADY_PROCESSED, exception.errorCode());
    }

    private AdminPasswordResetRequestUseCase useCase() {
        return new AdminPasswordResetRequestUseCase(
                passwordResetRequestRepositoryPort,
                userRepositoryPort,
                passwordEncoder,
                adminAuditLogRepositoryPort,
                transactionOperations,
                tokenStateRepositoryPort,
                objectMapper);
    }

    @SuppressWarnings("unchecked")
    private void executeTransactionCallback() {
        given(transactionOperations.execute(any()))
                .willAnswer(
                        invocation ->
                                ((org.springframework.transaction.support.TransactionCallback<Object>)
                                                invocation.getArgument(0))
                                        .doInTransaction(null));
    }

    private PasswordResetRequest request(PasswordResetRequestStatus status) {
        Instant now = Instant.parse("2026-06-23T00:00:00Z");
        return new PasswordResetRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "User One",
                "user1",
                "user1@example.com",
                status,
                now,
                status == PasswordResetRequestStatus.PENDING ? null : now.plusSeconds(60),
                status == PasswordResetRequestStatus.PENDING ? null : UUID.randomUUID(),
                now,
                now);
    }

    private User user(UUID userId, UserRole role) {
        return User.of(
                userId,
                "user1",
                passwordEncoder.encode("old-password"),
                "User One",
                "user1@example.com",
                role,
                UserStatus.ACTIVE,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-01T00:00:00Z"));
    }
}
