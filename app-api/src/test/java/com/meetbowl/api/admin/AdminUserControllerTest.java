package com.meetbowl.api.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.AuthenticatedUserAttributes;
import com.meetbowl.api.common.auth.AuthenticatedUserRole;
import com.meetbowl.application.admin.ResetUserPasswordUseCase;
import com.meetbowl.application.auth.AccessTokenValidationService;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ResetUserPasswordUseCase resetUserPasswordUseCase;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;

    @Test
    void resetPasswordSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AuthenticatedUser admin =
                new AuthenticatedUser(
                        adminId, UUID.randomUUID(), AuthenticatedUserRole.ADMIN, "관리자");

        mockMvc.perform(
                        post("/api/v1/admin/users/{userId}/password/reset", userId)
                                .requestAttr(AuthenticatedUserAttributes.CURRENT_USER, admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "newPassword": "newPassword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(resetUserPasswordUseCase).execute(any());
    }

    @Test
    void resetPasswordFailsWhenNotAdmin() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user =
                new AuthenticatedUser(
                        UUID.randomUUID(), UUID.randomUUID(), AuthenticatedUserRole.USER, "일반사용자");

        mockMvc.perform(
                        post("/api/v1/admin/users/{userId}/password/reset", userId)
                                .requestAttr(AuthenticatedUserAttributes.CURRENT_USER, user)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "newPassword": "newPassword123"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));
    }
}
