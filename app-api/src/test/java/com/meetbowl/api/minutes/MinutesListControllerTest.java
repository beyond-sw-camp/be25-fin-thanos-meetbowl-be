package com.meetbowl.api.minutes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import com.meetbowl.application.minutes.AddMinutesFavoriteUseCase;
import com.meetbowl.application.minutes.GetMinutesListUseCase;
import com.meetbowl.application.minutes.MinutesListItemResult;
import com.meetbowl.application.minutes.RemoveMinutesFavoriteUseCase;

@WebMvcTest(controllers = MinutesListController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CurrentUserArgumentResolver.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class MinutesListControllerTest {

    private static final UUID MEETING_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MINUTES_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID ORGANIZATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final Instant APPROVED_AT = Instant.parse("2099-01-01T01:00:00Z");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;
    @MockitoBean private GlobalPermissionChecker globalPermissionChecker;
    @MockitoBean private GetMinutesListUseCase getMinutesListUseCase;
    @MockitoBean private AddMinutesFavoriteUseCase addMinutesFavoriteUseCase;
    @MockitoBean private RemoveMinutesFavoriteUseCase removeMinutesFavoriteUseCase;

    @Test
    void getMinutesList() throws Exception {
        given(getMinutesListUseCase.execute(any(), any(), any()))
                .willReturn(
                        List.of(
                                new MinutesListItemResult(
                                        MINUTES_ID,
                                        MEETING_ID,
                                        ORGANIZATION_ID,
                                        USER_ID,
                                        "회의록",
                                        "APPROVED",
                                        "Meeting summary",
                                        APPROVED_AT,
                                        true)));

        mockMvc.perform(
                        get("/api/v1/minutes")
                                .param("keyword", "meeting")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].minutesId").value(MINUTES_ID.toString()))
                .andExpect(jsonPath("$.data[0].title").value("회의록"))
                .andExpect(jsonPath("$.data[0].favorite").value(true));

        verify(getMinutesListUseCase).execute(USER_ID, ORGANIZATION_ID, "meeting");
    }

    @Test
    void addFavorite() throws Exception {
        mockMvc.perform(
                        post("/api/v1/minutes/{minutesId}/favorite", MINUTES_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(addMinutesFavoriteUseCase).execute(USER_ID, ORGANIZATION_ID, MINUTES_ID);
    }

    @Test
    void removeFavorite() throws Exception {
        mockMvc.perform(
                        delete("/api/v1/minutes/{minutesId}/favorite", MINUTES_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(removeMinutesFavoriteUseCase).execute(USER_ID, ORGANIZATION_ID, MINUTES_ID);
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                USER_ID, ORGANIZATION_ID, AuthenticatedUserRole.USER, "Reviewer");
    }
}
