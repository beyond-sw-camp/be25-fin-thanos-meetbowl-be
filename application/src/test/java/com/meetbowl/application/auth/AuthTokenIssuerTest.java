package com.meetbowl.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
