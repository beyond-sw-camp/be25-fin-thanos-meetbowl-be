package com.meetbowl.api.common.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.meetbowl.application.auth.AccessTokenValidationService;

class JwtAuthenticatedUserConverterTest {

    private final AccessTokenValidationService validationService =
            mock(AccessTokenValidationService.class);

    private final JwtAuthenticatedUserConverter converter =
            new JwtAuthenticatedUserConverter(validationService);

    @Test
    void convertsJwtClaimsToAuthenticatedUserDetails() {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        given(validationService.isRevoked(any(), any(), any())).willReturn(false);
        Jwt jwt =
                jwtBuilder(userId)
                        .claim("organizationId", organizationId.toString())
                        .claim("role", "USER")
                        .claim("displayName", "홍길동")
                        .build();

        JwtAuthenticationToken authentication = converter.convert(jwt);

        AuthenticatedUser user =
                assertInstanceOf(AuthenticatedUser.class, authentication.getDetails());
        assertEquals(userId, user.userId());
        assertEquals(organizationId, user.organizationId());
        assertEquals(AuthenticatedUserRole.USER, user.role());
        assertEquals("홍길동", user.displayName());
        assertEquals("token-id", user.accessTokenId());
        assertEquals("ROLE_USER", authentication.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void invalidRoleFailsJwtAuthentication() {
        Jwt jwt = jwtBuilder(UUID.randomUUID()).claim("role", "OWNER").build();

        assertThrows(BadJwtException.class, () -> converter.convert(jwt));
    }

    @Test
    void revokedAccessTokenFailsJwtAuthentication() {
        given(validationService.isRevoked(any(), any(), any())).willReturn(true);
        Jwt jwt = jwtBuilder(UUID.randomUUID()).claim("role", "USER").build();

        assertThrows(BadJwtException.class, () -> converter.convert(jwt));
    }

    @Test
    void tokenWithoutIssuedAtFailsJwtAuthentication() {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .headers(headers -> headers.putAll(Map.of("alg", "HS256")))
                        .expiresAt(Instant.now().plusSeconds(300))
                        .claim("jti", "token-id")
                        .claim("role", "USER")
                        .subject(UUID.randomUUID().toString())
                        .build();

        assertThrows(BadJwtException.class, () -> converter.convert(jwt));
    }

    @Test
    void initialPasswordChangeTokenGetsRoleAndPasswordChangeAuthorities() {
        Jwt jwt =
                jwtBuilder(UUID.randomUUID())
                        .claim("role", "ADMIN")
                        .claim("initialPasswordChangeRequired", true)
                        .build();

        JwtAuthenticationToken authentication = converter.convert(jwt);

        AuthenticatedUser user =
                assertInstanceOf(AuthenticatedUser.class, authentication.getDetails());
        assertEquals(true, user.initialPasswordChangeRequired());
        assertEquals(
                java.util.Set.of("ROLE_ADMIN", "ROLE_PASSWORD_CHANGE_REQUIRED"),
                authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .collect(Collectors.toSet()));
    }

    @Test
    void systemRoleJwtFailsAuthentication() {
        Jwt jwt = jwtBuilder(UUID.randomUUID()).claim("role", "SYSTEM").build();

        assertThrows(BadJwtException.class, () -> converter.convert(jwt));
    }

    private Jwt.Builder jwtBuilder(UUID userId) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .headers(headers -> headers.putAll(Map.of("alg", "HS256")))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("jti", "token-id")
                .subject(userId.toString());
    }
}
