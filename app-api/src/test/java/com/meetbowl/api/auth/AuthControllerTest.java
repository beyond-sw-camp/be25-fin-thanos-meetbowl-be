package com.meetbowl.api.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.api.auth.dto.ChangeInitialPasswordRequest;
import com.meetbowl.api.auth.dto.LoginRequest;
import com.meetbowl.api.auth.dto.LogoutRequest;
import com.meetbowl.api.auth.dto.PasswordResetRequest;
import com.meetbowl.api.auth.dto.RefreshTokenRequest;
import com.meetbowl.api.common.GlobalExceptionHandler;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.AuthenticatedUserAttributes;
import com.meetbowl.api.common.auth.AuthenticatedUserRole;
import com.meetbowl.api.common.auth.CurrentUserArgumentResolver;
import com.meetbowl.api.config.WebMvcConfig;
import com.meetbowl.application.auth.AccessTokenValidationService;
import com.meetbowl.application.auth.ChangeInitialPasswordUseCase;
import com.meetbowl.application.auth.IssuedTokens;
import com.meetbowl.application.auth.LoginResult;
import com.meetbowl.application.auth.LoginUseCase;
import com.meetbowl.application.auth.LogoutUseCase;
import com.meetbowl.application.auth.PasswordResetRequestUseCase;
import com.meetbowl.application.auth.RefreshTokenUseCase;

@WebMvcTest(AuthController.class)
@Import({CurrentUserArgumentResolver.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private LoginUseCase loginUseCase;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;
    @MockitoBean private RefreshTokenUseCase refreshTokenUseCase;
    @MockitoBean private LogoutUseCase logoutUseCase;
    @MockitoBean private ChangeInitialPasswordUseCase changeInitialPasswordUseCase;
    @MockitoBean private PasswordResetRequestUseCase passwordResetRequestUseCase;

    @Test
    @DisplayName("濡쒓렇???깃났")
    void loginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("user1", "password");
        UUID userId = UUID.randomUUID();
        LoginResult result =
                new LoginResult(
                        "access-token",
                        "refresh-token",
                        "Bearer",
                        900L,
                        1209600L,
                        new LoginResult.UserSummary(
                                userId,
                                "user1",
                                "Tester",
                                "user1@test.com",
                                null,
                                null,
                                "Affiliate",
                                "Department",
                                "Team",
                                "Position",
                                false));
        given(loginUseCase.execute(any())).willReturn(result);

        ObjectMapper objectMapper = new ObjectMapper();
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.user.name").value("Tester"))
                .andExpect(jsonPath("$.data.user.initialPasswordChangeRequired").value(false));
    }

    @Test
    void refreshSuccess() throws Exception {
        given(refreshTokenUseCase.execute(any()))
                .willReturn(
                        new IssuedTokens("new-access", "new-refresh", "Bearer", 900L, 1209600L));

        mockMvc.perform(
                        post("/api/v1/auth/token/refresh")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        new ObjectMapper()
                                                .writeValueAsString(
                                                        new RefreshTokenRequest("refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
    }

    @Test
    void logoutSuccess() throws Exception {
        willDoNothing().given(logoutUseCase).execute(any());
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(
                        UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                        UUID.fromString("223e4567-e89b-12d3-a456-426614174000"),
                        AuthenticatedUserRole.USER,
                        "Tester",
                        "access-token-id",
                        Instant.now().plusSeconds(300));

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        new ObjectMapper()
                                                .writeValueAsString(
                                                        new LogoutRequest("refresh-token")))
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void requestPasswordResetSuccess() throws Exception {
        willDoNothing().given(passwordResetRequestUseCase).execute(any());

        mockMvc.perform(
                        post("/api/v1/auth/password-reset/request")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        new ObjectMapper()
                                                .writeValueAsString(
                                                        new PasswordResetRequest(
                                                                " user1 ",
                                                                " user1@local.meetbowl "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.message").value(PasswordResetRequestUseCase.ACCEPTED_MESSAGE));
    }

    @Test
    void changeInitialPasswordSuccess() throws Exception {
        given(changeInitialPasswordUseCase.execute(any()))
                .willReturn(
                        new IssuedTokens("new-access", "new-refresh", "Bearer", 900L, 1209600L));
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        AuthenticatedUserRole.USER,
                        "Tester",
                        "restricted-token-id",
                        Instant.now().plusSeconds(300),
                        true);

        mockMvc.perform(
                        post("/api/v1/auth/password/change-initial")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        new ObjectMapper()
                                                .writeValueAsString(
                                                        new ChangeInitialPasswordRequest(
                                                                "new-password", "new-password")))
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
    }

    @Test
    void changeInitialPasswordFailsWhenConfirmationDoesNotMatch() throws Exception {
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        AuthenticatedUserRole.USER,
                        "Tester",
                        "restricted-token-id",
                        Instant.now().plusSeconds(300),
                        true);

        mockMvc.perform(
                        post("/api/v1/auth/password/change-initial")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        new ObjectMapper()
                                                .writeValueAsString(
                                                        new ChangeInitialPasswordRequest(
                                                                "new-password",
                                                                "different-password")))
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
}
