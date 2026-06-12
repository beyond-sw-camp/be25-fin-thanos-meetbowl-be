package com.meetbowl.api.minutes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.meetbowl.application.minutes.ApproveMinutesCommand;
import com.meetbowl.application.minutes.ApproveMinutesUseCase;
import com.meetbowl.application.minutes.MinutesResult;
import com.meetbowl.application.minutes.ReviseMinutesCommand;
import com.meetbowl.application.minutes.ReviseMinutesUseCase;

@WebMvcTest(controllers = MinutesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CurrentUserArgumentResolver.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class MinutesControllerTest {

    private static final UUID MEETING_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MINUTES_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID ORGANIZATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final Instant APPROVED_AT = Instant.parse("2099-01-01T01:00:00Z");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;
    @MockitoBean private GlobalPermissionChecker globalPermissionChecker;
    @MockitoBean private ReviseMinutesUseCase reviseMinutesUseCase;
    @MockitoBean private ApproveMinutesUseCase approveMinutesUseCase;

    @Test
    void reviseMinutes() throws Exception {
        given(reviseMinutesUseCase.execute(any()))
                .willReturn(result("IN_REVIEW", "Updated summary", null));

        mockMvc.perform(
                        patch("/api/v1/meetings/{meetingId}/minutes", MEETING_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"summary\":\"Updated summary\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.data.summary").value("Updated summary"));

        ArgumentCaptor<ReviseMinutesCommand> commandCaptor =
                ArgumentCaptor.forClass(ReviseMinutesCommand.class);
        verify(reviseMinutesUseCase).execute(commandCaptor.capture());
        assertEquals(MEETING_ID, commandCaptor.getValue().meetingId());
        assertEquals(USER_ID, commandCaptor.getValue().actorUserId());
        assertEquals(ORGANIZATION_ID, commandCaptor.getValue().actorOrganizationId());
    }

    @Test
    void rejectBlankSummary() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/meetings/{meetingId}/minutes", MEETING_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"summary\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void approveMinutes() throws Exception {
        given(approveMinutesUseCase.execute(any()))
                .willReturn(result("APPROVED", "Meeting summary", APPROVED_AT));

        mockMvc.perform(
                        post("/api/v1/meetings/{meetingId}/minutes/approve", MEETING_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedAt").value("2099-01-01T01:00:00Z"));

        ArgumentCaptor<ApproveMinutesCommand> commandCaptor =
                ArgumentCaptor.forClass(ApproveMinutesCommand.class);
        verify(approveMinutesUseCase).execute(commandCaptor.capture());
        assertEquals(MEETING_ID, commandCaptor.getValue().meetingId());
        assertEquals(USER_ID, commandCaptor.getValue().actorUserId());
        assertEquals(ORGANIZATION_ID, commandCaptor.getValue().actorOrganizationId());
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                USER_ID, ORGANIZATION_ID, AuthenticatedUserRole.USER, "Reviewer");
    }

    private static MinutesResult result(String status, String summary, Instant approvedAt) {
        return new MinutesResult(
                MINUTES_ID, MEETING_ID, ORGANIZATION_ID, USER_ID, status, summary, approvedAt);
    }
}
