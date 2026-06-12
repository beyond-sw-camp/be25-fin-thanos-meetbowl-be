package com.meetbowl.api.common.auth;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.meetbowl.application.auth.AccessTokenValidationService;

/** JWT claim을 Spring Security 인증 객체와 Controller용 AuthenticatedUser로 변환한다. */
@Component
public class JwtAuthenticatedUserConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private static final String ORGANIZATION_ID_CLAIM = "organizationId";
    private static final String ROLE_CLAIM = "role";
    private static final String DISPLAY_NAME_CLAIM = "displayName";
    private static final String NAME_CLAIM = "name";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String INITIAL_PASSWORD_CHANGE_REQUIRED_CLAIM =
            "initialPasswordChangeRequired";
    private static final String PASSWORD_CHANGE_REQUIRED_AUTHORITY =
            ROLE_PREFIX + "PASSWORD_CHANGE_REQUIRED";

    private final AccessTokenValidationService accessTokenValidationService;

    public JwtAuthenticatedUserConverter(
            AccessTokenValidationService accessTokenValidationService) {
        this.accessTokenValidationService = accessTokenValidationService;
    }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        AuthenticatedUser authenticatedUser = toAuthenticatedUser(jwt);
        Collection<GrantedAuthority> authorities =
                authenticatedUser.initialPasswordChangeRequired()
                        ? List.of(new SimpleGrantedAuthority(PASSWORD_CHANGE_REQUIRED_AUTHORITY))
                        : List.of(
                                new SimpleGrantedAuthority(
                                        ROLE_PREFIX + authenticatedUser.role().name()));

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt, authorities, authenticatedUser.userId().toString());
        authentication.setDetails(authenticatedUser);
        return authentication;
    }

    private AuthenticatedUser toAuthenticatedUser(Jwt jwt) {
        UUID userId = parseUuid(jwt.getSubject(), "sub");
        UUID organizationId = parseOptionalUuid(jwt.getClaimAsString(ORGANIZATION_ID_CLAIM));
        AuthenticatedUserRole role = parseRole(jwt.getClaimAsString(ROLE_CLAIM));
        if (role == AuthenticatedUserRole.SYSTEM) {
            throw new BadJwtException("SYSTEM role JWT is not allowed.");
        }
        String displayName = resolveDisplayName(jwt, userId);
        String tokenId = requireTokenId(jwt.getId());
        boolean initialPasswordChangeRequired =
                Boolean.TRUE.equals(jwt.getClaimAsBoolean(INITIAL_PASSWORD_CHANGE_REQUIRED_CLAIM));
        if (jwt.getIssuedAt() == null) {
            throw new BadJwtException("iat claim is required.");
        }
        if (accessTokenValidationService.isRevoked(tokenId, userId, jwt.getIssuedAt())) {
            throw new BadJwtException("Access Token is revoked.");
        }

        return new AuthenticatedUser(
                userId,
                organizationId,
                role,
                displayName,
                tokenId,
                jwt.getExpiresAt(),
                initialPasswordChangeRequired);
    }

    private UUID parseUuid(String value, String claimName) {
        if (value == null || value.isBlank()) {
            throw new BadJwtException(claimName + " claim is required.");
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new BadJwtException(claimName + " claim must be UUID.", exception);
        }
    }

    private UUID parseOptionalUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return parseUuid(value, ORGANIZATION_ID_CLAIM);
    }

    private String requireTokenId(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new BadJwtException("jti claim is required.");
        }
        return tokenId;
    }

    private AuthenticatedUserRole parseRole(String value) {
        if (value == null || value.isBlank()) {
            throw new BadJwtException(ROLE_CLAIM + " claim is required.");
        }

        try {
            return AuthenticatedUserRole.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadJwtException(ROLE_CLAIM + " claim is invalid.", exception);
        }
    }

    private String resolveDisplayName(Jwt jwt, UUID userId) {
        String displayName = jwt.getClaimAsString(DISPLAY_NAME_CLAIM);
        if (displayName == null || displayName.isBlank()) {
            displayName = jwt.getClaimAsString(NAME_CLAIM);
        }

        if (displayName == null || displayName.isBlank()) {
            return userId.toString();
        }

        return displayName;
    }
}
