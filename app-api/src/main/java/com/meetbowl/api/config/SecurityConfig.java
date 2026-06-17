package com.meetbowl.api.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.meetbowl.api.common.auth.JwtAuthenticatedUserConverter;
import com.meetbowl.api.common.security.ApiAccessDeniedHandler;
import com.meetbowl.api.common.security.ApiAuthenticationEntryPoint;
import com.meetbowl.api.common.security.InternalTokenAuthenticationFilter;

/** JWT 기반 인증을 전역으로 적용한다. Controller는 인증 처리 대신 @CurrentUser로 검증된 사용자만 전달받는다. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String JWT_ISSUER = "meetbowl";

    /** SSE 구독 경로. EventSource가 Authorization 헤더를 못 붙이므로 이 경로에 한해 access token을 쿼리 파라미터로 받는다. */
    private static final String SSE_SUBSCRIBE_PATH = "/api/v1/notifications/subscribe";

    private static final String SSE_TOKEN_PARAM = "token";

    private static final String[] PUBLIC_ENDPOINTS = {
        "/error",
        "/api/v1/health",
        "/api/v1/auth/login",
        "/api/v1/auth/token/refresh",
        "/api/v1/meetings/*/join",
        "/api/v1/meetings/guest-join",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };

    private static final String[] SYSTEM_ENDPOINTS = {
        "/api/v1/internal/**", "/api/v1/meetings/*/minutes/share/participants"
    };

    private static final String[] ADMIN_ENDPOINTS = {
        "/api/v1/admin/**", "/api/v1/mails/announcements"
    };

    private static final String[] USER_OR_ADMIN_USER_ENDPOINTS = {
        "/api/v1/users/me",
        "/api/v1/users/me/settings",
        "/api/v1/users/search",
        "/api/v1/users/recipients/search",
        "/api/v1/users/*/simple-profile",
        "/api/v1/organization/users/*/summary"
    };

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticatedUserConverter jwtAuthenticatedUserConverter,
            ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
            ApiAccessDeniedHandler apiAccessDeniedHandler,
            InternalTokenAuthenticationFilter internalTokenAuthenticationFilter,
            BearerTokenResolver bearerTokenResolver)
            throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        exception ->
                                exception
                                        .authenticationEntryPoint(apiAuthenticationEntryPoint)
                                        .accessDeniedHandler(apiAccessDeniedHandler))
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers(PUBLIC_ENDPOINTS)
                                        .permitAll()
                                        .requestMatchers(SYSTEM_ENDPOINTS)
                                        .hasRole("SYSTEM")
                                        .requestMatchers(ADMIN_ENDPOINTS)
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/v1/auth/password/change-initial")
                                        .hasRole("PASSWORD_CHANGE_REQUIRED")
                                        .requestMatchers("/api/v1/auth/password/reset-request")
                                        .hasRole("USER")
                                        .requestMatchers(
                                                HttpMethod.GET, USER_OR_ADMIN_USER_ENDPOINTS)
                                        .hasAnyRole("USER", "ADMIN")
                                        .requestMatchers(
                                                HttpMethod.PATCH, "/api/v1/users/me/settings")
                                        .hasAnyRole("USER", "ADMIN")
                                        .requestMatchers(
                                                HttpMethod.PATCH, "/api/v1/users/me/password")
                                        .hasAnyRole("USER", "ADMIN")
                                        .requestMatchers(HttpMethod.PATCH, "/api/v1/users/me")
                                        .hasAnyRole("USER", "ADMIN")
                                        .requestMatchers("/api/v1/users/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers(HttpMethod.GET, "/api/v1/organizations")
                                        .hasAnyRole("USER", "ADMIN")
                                        .requestMatchers("/api/v1/organizations/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/v1/**")
                                        .hasAnyRole("USER", "ADMIN")
                                        .anyRequest()
                                        .denyAll())
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.bearerTokenResolver(bearerTokenResolver)
                                        .jwt(
                                                jwt ->
                                                        jwt.jwtAuthenticationConverter(
                                                                jwtAuthenticatedUserConverter)))
                .addFilterBefore(
                        internalTokenAuthenticationFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    /**
     * Authorization 헤더의 Bearer 토큰을 우선 사용하고, 헤더가 없을 때만 SSE 구독 경로에 한해 {@code ?token=} 쿼리 파라미터를 토큰으로
     * 받아들인다. 토큰을 URL에 노출하는 건 로그 유출 위험이 있어, EventSource가 헤더를 못 붙이는 SSE 구독에만 한정한다.
     */
    @Bean
    BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
        return request -> {
            String headerToken = headerResolver.resolve(request);
            if (headerToken != null) {
                return headerToken;
            }
            if (request.getRequestURI().endsWith(SSE_SUBSCRIBE_PATH)) {
                return request.getParameter(SSE_TOKEN_PARAM);
            }
            return null;
        };
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${meetbowl.security.jwt.secret}") String jwtSecret) {
        String secret = jwtSecret;
        if (jwtSecret == null || jwtSecret.isBlank()) {
            secret = "meetbowl-local-development-secret-key-32bytes";
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret key (meetbowl.security.jwt.secret) must be at least 32 bytes (256 bits) long.");
        }
        SecretKeySpec secretKey =
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JWT_ISSUER));
        return decoder;
    }

    @Bean
    InternalTokenAuthenticationFilter internalTokenAuthenticationFilter(
            @Value("${meetbowl.security.internal-token:}") String internalToken,
            ApiAuthenticationEntryPoint apiAuthenticationEntryPoint) {
        String token = internalToken;
        if (token == null || token.isBlank()) {
            token = "meetbowl-test-internal-token-value-32bytes";
        }
        if (token.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "Internal token must be at least 32 bytes (256 bits) long.");
        }
        return new InternalTokenAuthenticationFilter(token, apiAuthenticationEntryPoint);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
