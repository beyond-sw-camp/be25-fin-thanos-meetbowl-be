package com.meetbowl.api.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.meetbowl.api.common.GlobalExceptionHandler;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.AuthenticatedUserAttributes;
import com.meetbowl.api.common.auth.AuthenticatedUserRole;
import com.meetbowl.api.common.auth.CurrentUserArgumentResolver;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.api.config.WebMvcConfig;
import com.meetbowl.application.admin.ResetUserPasswordResult;
import com.meetbowl.application.admin.ResetUserPasswordUseCase;
import com.meetbowl.application.auth.AccessTokenValidationService;

@WebMvcTest(AdminUserController.class)
@Import({
    CurrentUserArgumentResolver.class,
    GlobalExceptionHandler.class,
    WebMvcConfig.class,
    GlobalPermissionChecker.class
})
class AdminUserControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ResetUserPasswordUseCase resetUserPasswordUseCase;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;

    @Test
    void resetPasswordSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser admin =
                new AuthenticatedUser(
                        UUID.randomUUID(), UUID.randomUUID(), AuthenticatedUserRole.ADMIN, "admin");
        given(resetUserPasswordUseCase.execute(any()))
                .willReturn(new ResetUserPasswordResult("Temp1234Abcd5678"));

        mockMvc.perform(
                        post("/api/v1/admin/users/{userId}/password/reset", userId)
                                .requestAttr(AuthenticatedUserAttributes.CURRENT_USER, admin)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.temporaryPassword").value("Temp1234Abcd5678"));

        verify(resetUserPasswordUseCase).execute(any());
    }

    @Test
    void resetPasswordFailsWhenNotAdmin() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user =
                new AuthenticatedUser(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        AuthenticatedUserRole.USER,
                        "user");

        mockMvc.perform(
                        post("/api/v1/admin/users/{userId}/password/reset", userId)
                                .requestAttr(AuthenticatedUserAttributes.CURRENT_USER, user)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));

        verifyNoInteractions(resetUserPasswordUseCase);
    }
}
