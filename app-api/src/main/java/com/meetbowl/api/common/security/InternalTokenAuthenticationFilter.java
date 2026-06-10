package com.meetbowl.api.common.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.meetbowl.api.common.ApiHeaders;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.AuthenticatedUserRole;

/** 내부 서버 전용 경로에서 X-Internal-Token을 SYSTEM 인증으로 변환한다. */
public class InternalTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final UUID SYSTEM_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final List<String> INTERNAL_PATHS =
            List.of("/api/v1/internal/**", "/api/v1/meetings/*/minutes/share/participants");

    private final byte[] expectedToken;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    public InternalTokenAuthenticationFilter(
            String internalToken, ApiAuthenticationEntryPoint authenticationEntryPoint) {
        this.expectedToken = internalToken.getBytes(StandardCharsets.UTF_8);
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return INTERNAL_PATHS.stream()
                .noneMatch(path -> pathMatcher.match(path, request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String providedToken = request.getHeader(ApiHeaders.INTERNAL_TOKEN);
        if (providedToken == null || !tokenMatches(providedToken)) {
            securityContextHolderStrategy.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new org.springframework.security.authentication.BadCredentialsException(
                            "Invalid internal token."));
            return;
        }

        AuthenticatedUser systemUser =
                new AuthenticatedUser(
                        SYSTEM_USER_ID,
                        null,
                        AuthenticatedUserRole.SYSTEM,
                        "internal-system",
                        "internal-token",
                        Instant.MAX);
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        systemUser, null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM")));
        authentication.setDetails(systemUser);

        var context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        filterChain.doFilter(request, response);
    }

    private boolean tokenMatches(String providedToken) {
        return MessageDigest.isEqual(expectedToken, providedToken.getBytes(StandardCharsets.UTF_8));
    }
}
