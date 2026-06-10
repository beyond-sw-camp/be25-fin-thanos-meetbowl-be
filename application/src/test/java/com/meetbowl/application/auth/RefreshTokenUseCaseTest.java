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

    private User createUser() {
        return User.of(
                UUID.randomUUID(),
                "user1",
                "hash",
                "name",
                "email",
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
