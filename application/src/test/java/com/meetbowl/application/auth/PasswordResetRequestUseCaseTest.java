package com.meetbowl.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.auth.PasswordResetRequestRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class PasswordResetRequestUseCaseTest {

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private PasswordResetRequestRepositoryPort passwordResetRequestRepositoryPort;
    @Mock private AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void requestExistingAccount_recordsAuditWithoutExposingEmail() throws Exception {
        User user = createUser("user1", "user1@local.meetbowl");
        given(userRepositoryPort.findByLoginId("user1")).willReturn(Optional.of(user));
        given(passwordResetRequestRepositoryPort.save(any()))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(adminAuditLogRepositoryPort.save(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        PasswordResetRequestUseCase useCase =
                new PasswordResetRequestUseCase(
                        userRepositoryPort,
                        passwordResetRequestRepositoryPort,
                        adminAuditLogRepositoryPort,
                        objectMapper);
        useCase.execute(
                new PasswordResetRequestCommand(
                        " user1 ", " user1@local.meetbowl ", "127.0.0.1", "JUnit"));

        verify(passwordResetRequestRepositoryPort).save(any());
        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepositoryPort).save(captor.capture());
        assertEquals("PASSWORD_RESET_REQUEST", captor.getValue().actionName());
        assertEquals("user1", captor.getValue().targetLoginId());
        assertEquals("name", captor.getValue().targetName());
        JsonNode snapshot = objectMapper.readTree(captor.getValue().afterValue());
        assertEquals("PUBLIC_API", snapshot.get("requestSource").asText());
    }

    @Test
    void requestUnknownAccount_returnsSilentlyWithoutAudit() {
        given(userRepositoryPort.findByLoginId("ghost")).willReturn(Optional.empty());

        PasswordResetRequestUseCase useCase =
                new PasswordResetRequestUseCase(
                        userRepositoryPort,
                        passwordResetRequestRepositoryPort,
                        adminAuditLogRepositoryPort,
                        objectMapper);
        useCase.execute(
                new PasswordResetRequestCommand(
                        "ghost", "ghost@local.meetbowl", "127.0.0.1", "JUnit"));

        verify(passwordResetRequestRepositoryPort, never()).save(any());
        verify(adminAuditLogRepositoryPort, never()).save(any());
    }

    private User createUser(String loginId, String email) {
        return User.of(
                UUID.randomUUID(),
                loginId,
                "hash",
                "name",
                email,
                UserRole.USER,
                UserStatus.ACTIVE,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null);
    }
}
