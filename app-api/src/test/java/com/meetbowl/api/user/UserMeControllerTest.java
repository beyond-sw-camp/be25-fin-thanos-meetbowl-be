package com.meetbowl.api.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalTime;
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
import com.meetbowl.application.user.MyProfileResult;
import com.meetbowl.application.user.MyProfileUseCase;
import com.meetbowl.application.user.MySettingsResult;
import com.meetbowl.application.user.MySettingsUseCase;
import com.meetbowl.application.user.UpdateMyProfileCommand;

@WebMvcTest(UserMeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    CurrentUserArgumentResolver.class,
    GlobalExceptionHandler.class,
    WebMvcConfig.class,
    GlobalPermissionChecker.class
})
class UserMeControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ORGANIZATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MyProfileUseCase myProfileUseCase;
    @MockitoBean private MySettingsUseCase mySettingsUseCase;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;

    @Test
    void getProfileSuccess() throws Exception {
        given(myProfileUseCase.get(USER_ID)).willReturn(profileResult("USER"));

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.loginId").value("user01"))
                .andExpect(jsonPath("$.data.name").value("User One"))
                .andExpect(jsonPath("$.data.email").value("user01@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.affiliate").value("Affiliate"))
                .andExpect(jsonPath("$.data.department").value("Department"))
                .andExpect(jsonPath("$.data.team").value("Team"))
                .andExpect(jsonPath("$.data.position").value("Position"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void updateProfileSuccessOnlyPassesEditableFields() throws Exception {
        given(myProfileUseCase.update(any())).willReturn(profileResult("USER"));

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Updated User",
                                          "email": "updated@example.com",
                                          "role": "ADMIN",
                                          "status": "INACTIVE",
                                          "affiliateId": "00000000-0000-0000-0000-000000000003",
                                          "activeFrom": "2026-01-01T00:00:00Z",
                                          "passwordHash": "leak"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        ArgumentCaptor<UpdateMyProfileCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateMyProfileCommand.class);
        verify(myProfileUseCase).update(commandCaptor.capture());
        assertEquals(USER_ID, commandCaptor.getValue().userId());
        assertEquals("Updated User", commandCaptor.getValue().name());
        assertEquals("updated@example.com", commandCaptor.getValue().email());
    }

    @Test
    void getSettingsSuccess() throws Exception {
        given(mySettingsUseCase.get(USER_ID))
                .willReturn(new MySettingsResult(15, true, LocalTime.of(19, 30)));

        mockMvc.perform(
                        get("/api/v1/users/me/settings")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meetingStartReminderMinutes").value(15))
                .andExpect(jsonPath("$.data.autoBackupEnabled").value(true))
                .andExpect(jsonPath("$.data.autoBackupTime").value("19:30:00"));
    }

    @Test
    void updateSettingsSuccess() throws Exception {
        given(mySettingsUseCase.update(any()))
                .willReturn(new MySettingsResult(20, false, LocalTime.of(18, 0)));

        mockMvc.perform(
                        patch("/api/v1/users/me/settings")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "meetingStartReminderMinutes": 20,
                                          "autoBackupEnabled": false,
                                          "autoBackupTime": "18:00:00"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meetingStartReminderMinutes").value(20))
                .andExpect(jsonPath("$.data.autoBackupEnabled").value(false));

        ArgumentCaptor<MySettingsUseCase.UpdateMySettingsCommand> commandCaptor =
                ArgumentCaptor.forClass(MySettingsUseCase.UpdateMySettingsCommand.class);
        verify(mySettingsUseCase).update(commandCaptor.capture());
        assertEquals(USER_ID, commandCaptor.getValue().userId());
        assertEquals(20, commandCaptor.getValue().meetingStartReminderMinutes());
        assertEquals(false, commandCaptor.getValue().autoBackupEnabled());
    }

    @Test
    void updateSettingsFailsWhenReminderMinutesIsNegative() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/users/me/settings")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "meetingStartReminderMinutes": -1,
                                          "autoBackupEnabled": false,
                                          "autoBackupTime": "18:00:00"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(mySettingsUseCase);
    }

    @Test
    void adminCanAccessProfileApi() throws Exception {
        given(myProfileUseCase.get(USER_ID)).willReturn(profileResult("ADMIN"));

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    private AuthenticatedUser authenticatedUser(AuthenticatedUserRole role) {
        return new AuthenticatedUser(USER_ID, ORGANIZATION_ID, role, "User One");
    }

    private MyProfileResult profileResult(String role) {
        return new MyProfileResult(
                USER_ID,
                "user01",
                "User One",
                "user01@example.com",
                role,
                "ACTIVE",
                "Affiliate",
                "Department",
                "Team",
                "Position",
                Instant.parse("2026-06-11T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z"));
    }
}
