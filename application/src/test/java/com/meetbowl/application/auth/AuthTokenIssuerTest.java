package com.meetbowl.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.auth.JwtTokenProvider;
import com.meetbowl.domain.auth.TokenStateRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class AuthTokenIssuerTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TokenStateRepositoryPort tokenStateRepositoryPort;

    @Test
    void issue_success_saves_hashed_refresh_token() {
        User user = createUser();
        AuthTokenIssuer issuer =
                new AuthTokenIssuer(jwtTokenProvider, tokenStateRepositoryPort, 120L);
        given(jwtTokenProvider.createAccessToken(eq(user.id().toString()), any(), any()))
                .willReturn("access-token");
        given(jwtTokenProvider.getAccessTokenExpirationSeconds()).willReturn(30L);

        IssuedTokens tokens = issuer.issue(user);

        ArgumentCaptor<String> tokenHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(tokenStateRepositoryPort)
                .saveRefreshToken(
                        tokenHashCaptor.capture(), eq(user.id()), eq(Duration.ofSeconds(120L)));
        assertEquals("access-token", tokens.accessToken());
        assertEquals(30L, tokens.accessTokenExpiresIn());
        assertNotEquals(tokens.refreshToken(), tokenHashCaptor.getValue());
        assertEquals(RefreshTokenHasher.hash(tokens.refreshToken()), tokenHashCaptor.getValue());
    }

    @Test
    void issueInitialPasswordChangeToken_doesNotIssueRefreshToken() {
        User user = createUser(true);
        AuthTokenIssuer issuer =
                new AuthTokenIssuer(jwtTokenProvider, tokenStateRepositoryPort, 120L);
        given(jwtTokenProvider.createAccessToken(eq(user.id().toString()), any(), any()))
                .willReturn("restricted-access-token");
        given(jwtTokenProvider.getAccessTokenExpirationSeconds()).willReturn(30L);

        IssuedTokens tokens = issuer.issueInitialPasswordChangeToken(user);

        assertEquals("restricted-access-token", tokens.accessToken());
        assertEquals(null, tokens.refreshToken());
        assertEquals(0L, tokens.refreshTokenExpiresIn());
        verify(tokenStateRepositoryPort, never()).saveRefreshToken(any(), any(), any());
    }

    @Test
    void issue_fails_for_system_account() {
        User user = createUser(false, UserRole.SYSTEM);
        AuthTokenIssuer issuer =
                new AuthTokenIssuer(jwtTokenProvider, tokenStateRepositoryPort, 120L);

        BusinessException exception =
                assertThrows(BusinessException.class, () -> issuer.issue(user));

        assertEquals(ErrorCode.COMMON_UNAUTHORIZED, exception.errorCode());
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
