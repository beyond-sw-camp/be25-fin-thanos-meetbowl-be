package com.meetbowl.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionOperations;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.auth.TokenStateRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class ChangeInitialPasswordUseCaseTest {

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthTokenIssuer authTokenIssuer;
    @Mock private TokenStateRepositoryPort tokenStateRepositoryPort;
    @Mock private TransactionOperations transactionOperations;

    @Test
    void changeInitialPassword_updatesPasswordRevokesRestrictedTokenAndIssuesNormalTokens() {
        User user = createUser(true);
        Instant expiresAt = Instant.now().plusSeconds(300);
        IssuedTokens issuedTokens = new IssuedTokens("access", "refresh", "Bearer", 900L, 1209600L);
        ChangeInitialPasswordUseCase useCase = useCase();
        given(userRepositoryPort.findById(user.id())).willReturn(Optional.of(user));
        executeTransactionCallback();
        given(passwordEncoder.matches("new-password", user.passwordHash())).willReturn(false);
        given(passwordEncoder.encode("new-password")).willReturn("new-password-hash");
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(authTokenIssuer.issue(any())).willReturn(issuedTokens);

        IssuedTokens result =
                useCase.execute(
                        new ChangeInitialPasswordCommand(
                                user.id(), "new-password", "restricted-token-id", expiresAt));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(savedUser.capture());
        assertEquals("new-password-hash", savedUser.getValue().passwordHash());
        assertFalse(savedUser.getValue().initialPasswordChangeRequired());
        verify(tokenStateRepositoryPort).revokeAccessToken(eq("restricted-token-id"), any());
        verify(authTokenIssuer).issue(savedUser.getValue());
        assertEquals(issuedTokens, result);
    }

    @Test
    void changeInitialPassword_failsWhenNewPasswordMatchesExistingPassword() {
        User user = createUser(true);
        ChangeInitialPasswordUseCase useCase = useCase();
        given(userRepositoryPort.findById(user.id())).willReturn(Optional.of(user));
        executeTransactionCallback();
        given(passwordEncoder.matches("same-password", user.passwordHash())).willReturn(true);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new ChangeInitialPasswordCommand(
                                                user.id(),
                                                "same-password",
                                                "restricted-token-id",
                                                Instant.now().plusSeconds(300))));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void changeInitialPassword_failsWhenNewPasswordIsTooShort() {
        ChangeInitialPasswordUseCase useCase = useCase();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new ChangeInitialPasswordCommand(
                                                UUID.randomUUID(),
                                                "short",
                                                "restricted-token-id",
                                                Instant.now().plusSeconds(300))));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    private ChangeInitialPasswordUseCase useCase() {
        return new ChangeInitialPasswordUseCase(
                userRepositoryPort,
                passwordEncoder,
                authTokenIssuer,
                tokenStateRepositoryPort,
                transactionOperations);
    }

    @SuppressWarnings("unchecked")
    private void executeTransactionCallback() {
        given(transactionOperations.execute(any()))
                .willAnswer(
                        invocation ->
                                ((org.springframework.transaction.support.TransactionCallback<User>)
                                                invocation.getArgument(0))
                                        .doInTransaction(null));
    }

    private User createUser(boolean initialPasswordChangeRequired) {
        return User.of(
                UUID.randomUUID(),
                "user1",
                "old-password-hash",
                "name",
                "email",
                UserRole.USER,
                UserStatus.ACTIVE,
                null,
                null,
                null,
                null,
                initialPasswordChangeRequired,
                null,
                null,
                null,
                null);
    }
}
