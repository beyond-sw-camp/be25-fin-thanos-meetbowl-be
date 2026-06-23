package com.meetbowl.api.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import com.meetbowl.application.auth.AccessTokenValidationService;
import com.meetbowl.application.mail.MailRetentionPolicyCommand;
import com.meetbowl.application.mail.MailRetentionPolicyResult;
import com.meetbowl.application.mail.MailRetentionPolicyUseCase;

@WebMvcTest(MailRetentionPolicyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    CurrentUserArgumentResolver.class,
    GlobalExceptionHandler.class,
    WebMvcConfig.class,
    GlobalPermissionChecker.class
})
class MailRetentionPolicyControllerTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID ORGANIZATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final Instant UPDATED_AT = Instant.parse("2026-06-12T00:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MailRetentionPolicyUseCase mailRetentionPolicyUseCase;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;

    @Test
    void getRetentionPolicySuccessWhenAdmin() throws Exception {
        given(mailRetentionPolicyUseCase.get())
                .willReturn(new MailRetentionPolicyResult(365, true, UPDATED_AT, ADMIN_ID));

        mockMvc.perform(
                        get("/api/v1/admin/mail/retention-policy")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retentionDays").value(365))
                .andExpect(jsonPath("$.data.autoDeleteEnabled").value(true))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-06-12T00:00:00Z"))
                .andExpect(jsonPath("$.data.updatedBy").value(ADMIN_ID.toString()));
    }

    @Test
    void updateRetentionPolicySuccessWhenAdmin() throws Exception {
        given(mailRetentionPolicyUseCase.update(any()))
                .willReturn(new MailRetentionPolicyResult(180, false, UPDATED_AT, ADMIN_ID));

        mockMvc.perform(
                        patch("/api/v1/admin/mail/retention-policy")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .header("User-Agent", "MailRetentionPolicyControllerTest")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "retentionDays": 180,
                                          "autoDeleteEnabled": false
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retentionDays").value(180))
                .andExpect(jsonPath("$.data.autoDeleteEnabled").value(false));

        ArgumentCaptor<MailRetentionPolicyCommand> captor =
                ArgumentCaptor.forClass(MailRetentionPolicyCommand.class);
        verify(mailRetentionPolicyUseCase).update(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(180, captor.getValue().retentionDays());
        org.junit.jupiter.api.Assertions.assertEquals(false, captor.getValue().autoDeleteEnabled());
        org.junit.jupiter.api.Assertions.assertEquals(ADMIN_ID, captor.getValue().adminId());
    }

    @Test
    void getRetentionPolicyFailsWhenUser() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/mail/retention-policy")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));

        verifyNoInteractions(mailRetentionPolicyUseCase);
    }

    @Test
    void updateRetentionPolicyFailsWhenUser() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/admin/mail/retention-policy")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"retentionDays\":180,\"autoDeleteEnabled\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));

        verifyNoInteractions(mailRetentionPolicyUseCase);
    }

    @Test
    void updateRetentionPolicyFailsWhenRetentionDaysIsZero() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/admin/mail/retention-policy")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"retentionDays\":0,\"autoDeleteEnabled\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(mailRetentionPolicyUseCase);
    }

    @Test
    void updateRetentionPolicyFailsWhenRetentionDaysExceedsMax() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/admin/mail/retention-policy")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"retentionDays\":3651,\"autoDeleteEnabled\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(mailRetentionPolicyUseCase);
    }

    private AuthenticatedUser authenticatedUser(AuthenticatedUserRole role) {
        return new AuthenticatedUser(ADMIN_ID, ORGANIZATION_ID, role, "Admin");
    }
}
