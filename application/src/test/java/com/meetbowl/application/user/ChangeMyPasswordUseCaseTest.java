package com.meetbowl.application.user;

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

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class ChangeMyPasswordUseCaseTest {

    @Mock private UserRepositoryPort userRepositoryPort;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void changePasswordSuccess_updatesHashAndClearsRequiredFlag() {
        User user = createUser(true, "1234");
        ChangeMyPasswordUseCase useCase =
                new ChangeMyPasswordUseCase(userRepositoryPort, passwordEncoder);
        given(userRepositoryPort.findById(user.id())).willReturn(Optional.of(user));
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(
                new ChangeMyPasswordCommand(
                        user.id(), "1234", "new-password-123", "new-password-123"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(savedUser.capture());
        assertFalse(savedUser.getValue().passwordHash().equals("new-password-123"));
        assertTrue(
                passwordEncoder.matches("new-password-123", savedUser.getValue().passwordHash()));
        assertFalse(savedUser.getValue().initialPasswordChangeRequired());
    }

    @Test
    void changePasswordFailsWhenCurrentPasswordIsInvalid() {
        User user = createUser(false, "1234");
        ChangeMyPasswordUseCase useCase =
                new ChangeMyPasswordUseCase(userRepositoryPort, passwordEncoder);
        given(userRepositoryPort.findById(user.id())).willReturn(Optional.of(user));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new ChangeMyPasswordCommand(
                                                user.id(),
                                                "wrong-password",
                                                "new-password-123",
                                                "new-password-123")));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void changePasswordFailsWhenConfirmationDoesNotMatch() {
        ChangeMyPasswordUseCase useCase =
                new ChangeMyPasswordUseCase(userRepositoryPort, passwordEncoder);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new ChangeMyPasswordCommand(
                                                UUID.randomUUID(),
                                                "1234",
                                                "new-password-123",
                                                "different-password")));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    private User createUser(boolean initialPasswordChangeRequired, String rawPassword) {
        return User.of(
                UUID.randomUUID(),
                "user01",
                passwordEncoder.encode(rawPassword),
                "User One",
                "user01@example.com",
                UserRole.USER,
                UserStatus.ACTIVE,
                null,
                null,
                null,
                null,
                initialPasswordChangeRequired,
                null,
                null,
                Instant.parse("2026-06-08T08:00:00Z"),
                Instant.parse("2026-06-08T08:00:00Z"));
    }
}
