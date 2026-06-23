package com.meetbowl.api.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.meetbowl.api.common.ApiHeaders;
import com.meetbowl.application.auth.AccessTokenValidationService;
import com.meetbowl.application.meeting.MeetingRealtimeSessionStarter;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    private static final String JWT_SECRET = "meetbowl-local-development-secret-key-32bytes";
    private static final String INTERNAL_TOKEN = "meetbowl-test-internal-token-value-32bytes";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;
    @MockitoBean private MeetingRealtimeSessionStarter meetingRealtimeSessionStarter;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void protectedEndpointWithoutTokenReturnsCommonUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("인증이 필요합니다."));
    }

    @Test
    void protectedEndpointWithValidJwtPassesSecurityFilter() throws Exception {
        String accessToken = createAccessToken("USER");

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void tokenWithoutExpectedIssuerIsRejected() throws Exception {
        String accessToken = createAccessToken("USER", false, null);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meetingJoinEndpointIsPublicButGeneralGuestAccessIsStillBlocked() throws Exception {
        mockMvc.perform(post("/api/v1/meetings/" + UUID.randomUUID() + "/join"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/mails/inbox"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_UNAUTHORIZED"));
    }

    @Test
    void publicEndpointsAreAccessible() throws Exception {
        mockMvc.perform(post("/api/v1/meetings/guest-join")).andExpect(status().isNotFound());
    }

    @Test
    void corsPreflightForMeetingJoinEndpointIsAllowedFromLocalFrontend() throws Exception {
        mockMvc.perform(
                        options("/api/v1/meetings/" + UUID.randomUUID() + "/join")
                                .header("Origin", "http://localhost:5173")
                                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void systemJwtCannotAccessSystemEndpoint() throws Exception {
        String accessToken = createAccessToken("SYSTEM");

        mockMvc.perform(
                        post("/api/v1/internal/mails/send")
                                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_UNAUTHORIZED"));
    }

    @Test
    void validInternalTokenCanAccessInternalEndpoint() throws Exception {
        mockMvc.perform(
                        post("/api/v1/internal/mails/send")
                                .header(ApiHeaders.INTERNAL_TOKEN, INTERNAL_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidInternalTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(
                        post("/api/v1/internal/mails/send")
                                .header(ApiHeaders.INTERNAL_TOKEN, "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_UNAUTHORIZED"));
    }

    @Test
    void missingInternalTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/internal/mails/send"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_UNAUTHORIZED"));
    }

    @Test
    void internalTokenDoesNotAuthenticateGeneralApi() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header(ApiHeaders.INTERNAL_TOKEN, INTERNAL_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_UNAUTHORIZED"));
    }

    @Test
    void internalTokenCanAccessInternalEndpoint() throws Exception {
        mockMvc.perform(
                        post("/api/v1/internal/mails/send")
                                .header(ApiHeaders.INTERNAL_TOKEN, INTERNAL_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCannotAccessInternalEndpoint() throws Exception {
        String accessToken = createAccessToken("USER");

        mockMvc.perform(
                        post("/api/v1/internal/mails/send")
                                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_UNAUTHORIZED"));
    }

    @Test
    void passwordResetRequestEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset/request"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/password/reset-request"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void initialPasswordChangeTokenCanOnlyAccessInitialPasswordChangeEndpoint() throws Exception {
        String accessToken = createAccessToken("ADMIN", true);

        mockMvc.perform(
                        post("/api/v1/auth/password/change-initial")
                                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        get("/api/v1/admin/dashboard")
                                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void initialPasswordChangeUserTokenCanStillAccessGeneralUserEndpoint() throws Exception {
        String accessToken = createAccessToken("USER", true);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void normalUserCannotAccessInitialPasswordChangeEndpoint() throws Exception {
        String accessToken = createAccessToken("USER");

        mockMvc.perform(
                        post("/api/v1/auth/password/change-initial")
                                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));
    }

    @Test
    void userCannotAccessAdminEndpoint() throws Exception {
        String accessToken = createAccessToken("USER");

        mockMvc.perform(
                        get("/api/v1/admin/dashboard")
                                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));
    }

    @Test
    void userCannotAccessAdminOnlyUserManagementEndpoint() throws Exception {
        String accessToken = createAccessToken("USER");

        mockMvc.perform(
                        get("/api/v1/admin/users/" + UUID.randomUUID())
                                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));
    }

    @Test
    void adminCanAccessAdminEndpoint() throws Exception {
        String accessToken = createAccessToken("ADMIN");

        mockMvc.perform(
                        get("/api/v1/admin/dashboard")
                                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonApiEndpointIsDeniedEvenWhenAuthenticated() throws Exception {
        String accessToken = createAccessToken("ADMIN");

        mockMvc.perform(get("/unconfigured").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));
    }

    private String createAccessToken(String role) throws Exception {
        return createAccessToken(role, false);
    }

    private String createAccessToken(String role, boolean initialPasswordChangeRequired)
            throws Exception {
        return createAccessToken(role, initialPasswordChangeRequired, "meetbowl");
    }

    private String createAccessToken(
            String role, boolean initialPasswordChangeRequired, String issuer) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claimsBuilder =
                new JWTClaimsSet.Builder()
                        .subject(UUID.randomUUID().toString())
                        .jwtID(UUID.randomUUID().toString())
                        .claim("organizationId", UUID.randomUUID().toString())
                        .claim("role", role)
                        .claim("initialPasswordChangeRequired", initialPasswordChangeRequired)
                        .claim("displayName", "Tester")
                        .issueTime(Date.from(now))
                        .expirationTime(Date.from(now.plusSeconds(300)));
        if (issuer != null) {
            claimsBuilder.issuer(issuer);
        }
        JWTClaimsSet claims = claimsBuilder.build();

        SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJwt.sign(new MACSigner(JWT_SECRET));
        return signedJwt.serialize();
    }
}
