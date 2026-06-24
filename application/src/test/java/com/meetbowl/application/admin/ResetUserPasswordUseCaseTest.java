package com.meetbowl.application.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.auth.TokenStateRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class ResetUserPasswordUseCaseTest {

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    @Mock private TransactionOperations transactionOperations;
    @Mock private TokenStateRepositoryPort tokenStateRepositoryPort;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void resetPassword_returnsTemporaryPassword_hashesStoredPassword_and_setsInitialPasswordFlag()
            throws Exception {
        User user = createUser();
        ResetUserPasswordUseCase useCase = useCase();
        given(userRepositoryPort.findById(user.id())).willReturn(Optional.of(user));
        executeTransactionCallback();
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(adminAuditLogRepositoryPort.save(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        ResetUserPasswordResult result =
                useCase.execute(
                        new ResetUserPasswordCommand(
                                user.id(), UUID.randomUUID(), "admin", "127.0.0.1", "Mozilla/5.0"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(savedUser.capture());
        assertTrue(savedUser.getValue().initialPasswordChangeRequired());
        assertFalse(savedUser.getValue().passwordHash().equals(result.temporaryPassword()));
        assertTrue(
                passwordEncoder.matches(
                        result.temporaryPassword(), savedUser.getValue().passwordHash()));
        ArgumentCaptor<AdminAuditLog> auditLogCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepositoryPort).save(auditLogCaptor.capture());
        assertEquals("user1", auditLogCaptor.getValue().targetLoginId());
        assertEquals("name", auditLogCaptor.getValue().targetName());
        JsonNode afterSnapshot = objectMapper.readTree(auditLogCaptor.getValue().afterValue());
        assertTrue(afterSnapshot.get("initialPasswordChangeRequired").asBoolean());
        assertFalse(auditLogCaptor.getValue().afterValue().contains(result.temporaryPassword()));
        assertFalse(auditLogCaptor.getValue().afterValue().contains("temporaryPassword"));
        assertFalse(auditLogCaptor.getValue().afterValue().contains("passwordHash"));
        verify(tokenStateRepositoryPort)
                .revokeUserSessions(org.mockito.ArgumentMatchers.eq(user.id()), any());
    }

    @Test
    void resetPassword_failsWhenUserDoesNotExist() {
        ResetUserPasswordUseCase useCase = useCase();
        UUID missingUserId = UUID.randomUUID();
        given(userRepositoryPort.findById(missingUserId)).willReturn(Optional.empty());
        executeTransactionCallback();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new ResetUserPasswordCommand(
                                                missingUserId,
                                                UUID.randomUUID(),
                                                "admin",
                                                "127.0.0.1",
                                                "Mozilla/5.0")));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode());
    }

    @Test
    void resetPassword_failsWhenTargetIsInitialAdmin() {
        User admin = createUser(UserRole.ADMIN);
        ResetUserPasswordUseCase useCase = useCase();
        given(userRepositoryPort.findById(admin.id())).willReturn(Optional.of(admin));
        executeTransactionCallback();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new ResetUserPasswordCommand(
                                                admin.id(),
                                                admin.id(),
                                                "admin",
                                                "127.0.0.1",
                                                "Mozilla/5.0")));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
    }

    private ResetUserPasswordUseCase useCase() {
        return new ResetUserPasswordUseCase(
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
                                ((org.springframework.transaction.support.TransactionCallback<
                                                        ResetUserPasswordResult>)
                                                invocation.getArgument(0))
                                        .doInTransaction(null));
    }

    private User createUser() {
        return createUser(UserRole.USER);
    }

    private User createUser(UserRole role) {
        return User.of(
                UUID.randomUUID(),
                "user1",
                passwordEncoder.encode("existing-password"),
                "name",
                "email",
                role,
                UserStatus.ACTIVE,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                Instant.parse("2026-06-08T08:00:00Z"),
                Instant.parse("2026-06-08T08:00:00Z"));
    }
}
