package com.meetbowl.api.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.meetbowl.api.common.auth.JwtAuthenticatedUserConverter;
import com.meetbowl.api.common.security.ApiAccessDeniedHandler;
import com.meetbowl.api.common.security.ApiAuthenticationEntryPoint;
import com.meetbowl.api.common.security.InternalTokenAuthenticationFilter;

/** Stateless JWT security configuration for app-api. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String JWT_ISSUER = "meetbowl";
    private static final String SSE_SUBSCRIBE_PATH = "/api/v1/notifications/subscribe";
    private static final String SSE_TOKEN_PARAM = "token";
    private static final List<String> DEFAULT_ALLOWED_ORIGIN_PATTERNS =
            List.of("http://localhost:*", "http://127.0.0.1:*");

    @Value("${meetbowl.cors.allowed-origin-patterns:}")
    private String corsAllowedOriginPatterns;

    private static final String[] PUBLIC_ENDPOINTS = {
        "/error",
        "/api/v1/health",
        "/api/v1/auth/login",
        "/api/v1/auth/token/refresh",
        "/api/v1/auth/password-reset/request",
        "/api/v1/auth/password/reset-request",
        "/api/v1/meetings/*/join",
        "/api/v1/meetings/guest-join",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };

    private static final String[] SYSTEM_ENDPOINTS = {"/api/v1/internal/**"};

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
                .cors(Customizer.withDefaults())
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

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(parseAllowedOriginPatterns(corsAllowedOriginPatterns));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private static List<String> parseAllowedOriginPatterns(String rawPatterns) {
        if (rawPatterns == null || rawPatterns.isBlank()) {
            return DEFAULT_ALLOWED_ORIGIN_PATTERNS;
        }
        List<String> patterns =
                Arrays.stream(rawPatterns.split(","))
                        .map(String::trim)
                        .filter(pattern -> !pattern.isEmpty())
                        .collect(Collectors.toList());
        return patterns.isEmpty() ? DEFAULT_ALLOWED_ORIGIN_PATTERNS : patterns;
    }
}
