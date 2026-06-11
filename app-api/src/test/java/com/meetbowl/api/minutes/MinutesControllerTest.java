package com.meetbowl.api.minutes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
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
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;

@WebMvcTest(controllers = MinutesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    CurrentUserArgumentResolver.class,
    GlobalExceptionHandler.class,
    MinutesControllerTest.TestUseCaseConfig.class,
    WebMvcConfig.class
})
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
    @Autowired private ReviseMinutesUseCase reviseMinutesUseCase;
    @Autowired private ApproveMinutesUseCase approveMinutesUseCase;

    @Test
    void reviseMinutes() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/meetings/{meetingId}/minutes", MEETING_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"summary":"수정된 회의 요약"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.data.summary").value("수정된 회의 요약"));

        ReviseMinutesCommand command =
                ((TestReviseMinutesUseCase) reviseMinutesUseCase).lastCommand;
        assertEquals(MEETING_ID, command.meetingId());
        assertEquals(USER_ID, command.actorUserId());
        assertEquals(ORGANIZATION_ID, command.actorOrganizationId());
    }

    @Test
    void rejectBlankSummary() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/meetings/{meetingId}/minutes", MEETING_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"summary":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void approveMinutes() throws Exception {
        mockMvc.perform(
                        post("/api/v1/meetings/{meetingId}/minutes/approve", MEETING_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedAt").value("2099-01-01T01:00:00Z"));

        ApproveMinutesCommand command =
                ((TestApproveMinutesUseCase) approveMinutesUseCase).lastCommand;
        assertEquals(MEETING_ID, command.meetingId());
        assertEquals(USER_ID, command.actorUserId());
        assertEquals(ORGANIZATION_ID, command.actorOrganizationId());
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(USER_ID, ORGANIZATION_ID, AuthenticatedUserRole.USER, "검토자");
    }

    @TestConfiguration
    static class TestUseCaseConfig {

        @Bean
        ReviseMinutesUseCase reviseMinutesUseCase() {
            return new TestReviseMinutesUseCase();
        }

        @Bean
        ApproveMinutesUseCase approveMinutesUseCase() {
            return new TestApproveMinutesUseCase();
        }
    }

    private static class TestReviseMinutesUseCase extends ReviseMinutesUseCase {

        private ReviseMinutesCommand lastCommand;

        TestReviseMinutesUseCase() {
            super(new EmptyMinutesRepository());
        }

        @Override
        public MinutesResult execute(ReviseMinutesCommand command) {
            lastCommand = command;
            return result("IN_REVIEW", command.summary(), null);
        }
    }

    private static class TestApproveMinutesUseCase extends ApproveMinutesUseCase {

        private ApproveMinutesCommand lastCommand;

        TestApproveMinutesUseCase() {
            super(
                    new EmptyMinutesRepository(),
                    event -> {},
                    Clock.fixed(APPROVED_AT, ZoneOffset.UTC));
        }

        @Override
        public MinutesResult execute(ApproveMinutesCommand command) {
            lastCommand = command;
            return result("APPROVED", "회의 요약", APPROVED_AT);
        }
    }

    private static MinutesResult result(String status, String summary, Instant approvedAt) {
        return new MinutesResult(
                MINUTES_ID, MEETING_ID, ORGANIZATION_ID, USER_ID, status, summary, approvedAt);
    }

    private static class EmptyMinutesRepository implements MinutesRepositoryPort {

        @Override
        public Minutes save(Minutes minutes) {
            return minutes;
        }

        @Override
        public Optional<Minutes> findById(UUID minutesId) {
            return Optional.empty();
        }

        @Override
        public Optional<Minutes> findByMeetingId(UUID meetingId) {
            return Optional.empty();
        }

        @Override
        public boolean existsByMeetingId(UUID meetingId) {
            return false;
        }
    }
}
