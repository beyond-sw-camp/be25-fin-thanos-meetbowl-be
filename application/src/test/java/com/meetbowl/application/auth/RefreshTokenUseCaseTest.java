package com.meetbowl.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.auth.TokenStateRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    @Mock private TokenStateRepositoryPort tokenStateRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private AuthTokenIssuer authTokenIssuer;

    @Test
    void refresh_success_consumes_old_token_and_issues_new_tokens() {
        String refreshToken = "refresh-token";
        User user = createUser();
        IssuedTokens issuedTokens =
                new IssuedTokens("access", "new-refresh", "Bearer", 900L, 1209600L);
        RefreshTokenUseCase useCase =
                new RefreshTokenUseCase(
                        tokenStateRepositoryPort, userRepositoryPort, authTokenIssuer);
        given(tokenStateRepositoryPort.consumeRefreshToken(RefreshTokenHasher.hash(refreshToken)))
                .willReturn(Optional.of(user.id()));
        given(userRepositoryPort.findById(user.id())).willReturn(Optional.of(user));
        given(authTokenIssuer.issue(user)).willReturn(issuedTokens);

        IssuedTokens result = useCase.execute(new RefreshTokenCommand(refreshToken));

        assertEquals(issuedTokens, result);
        verify(tokenStateRepositoryPort).consumeRefreshToken(RefreshTokenHasher.hash(refreshToken));
    }

    @Test
    void refresh_fail_when_token_is_invalid() {
        String refreshToken = "invalid-token";
        RefreshTokenUseCase useCase =
                new RefreshTokenUseCase(
                        tokenStateRepositoryPort, userRepositoryPort, authTokenIssuer);
        given(tokenStateRepositoryPort.consumeRefreshToken(RefreshTokenHasher.hash(refreshToken)))
                .willReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(new RefreshTokenCommand(refreshToken)));

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_INVALID, exception.errorCode());
    }

    @Test
    void refresh_fail_when_initial_password_change_is_required() {
        String refreshToken = "refresh-token";
        User user = createUser(true);
        RefreshTokenUseCase useCase =
                new RefreshTokenUseCase(
                        tokenStateRepositoryPort, userRepositoryPort, authTokenIssuer);
        given(tokenStateRepositoryPort.consumeRefreshToken(RefreshTokenHasher.hash(refreshToken)))
                .willReturn(Optional.of(user.id()));
        given(userRepositoryPort.findById(user.id())).willReturn(Optional.of(user));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(new RefreshTokenCommand(refreshToken)));

        assertEquals(ErrorCode.AUTH_INITIAL_PASSWORD_CHANGE_REQUIRED, exception.errorCode());
    }

    @Test
    void refresh_fail_when_user_is_system_account() {
        String refreshToken = "refresh-token";
        User user = createUser(false, UserRole.SYSTEM);
        RefreshTokenUseCase useCase =
                new RefreshTokenUseCase(
                        tokenStateRepositoryPort, userRepositoryPort, authTokenIssuer);
        given(tokenStateRepositoryPort.consumeRefreshToken(RefreshTokenHasher.hash(refreshToken)))
                .willReturn(Optional.of(user.id()));
        given(userRepositoryPort.findById(user.id())).willReturn(Optional.of(user));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(new RefreshTokenCommand(refreshToken)));

        assertEquals(ErrorCode.AUTH_REFRESH_TOKEN_INVALID, exception.errorCode());
    }

    private User createUser() {
        return createUser(false);
    }

    private User createUser(boolean initialPasswordChangeRequired) {
        return createUser(initialPasswordChangeRequired, UserRole.USER);
    }

    private User createUser(boolean initialPasswordChangeRequired, UserRole role) {
        return User.of(
                UUID.randomUUID(),
                "user1",
                "hash",
                "name",
                "email",
                role,
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
