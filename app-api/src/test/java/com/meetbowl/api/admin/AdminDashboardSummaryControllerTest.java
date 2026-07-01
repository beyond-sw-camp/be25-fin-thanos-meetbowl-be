package com.meetbowl.api.admin;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.meetbowl.application.admin.AdminDashboardSummaryResult;
import com.meetbowl.application.admin.AdminDashboardSummaryUseCase;
import com.meetbowl.application.auth.AccessTokenValidationService;
import com.meetbowl.application.mail.MailRetentionPolicyResult;

@WebMvcTest(AdminDashboardSummaryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    CurrentUserArgumentResolver.class,
    GlobalExceptionHandler.class,
    WebMvcConfig.class,
    GlobalPermissionChecker.class
})
class AdminDashboardSummaryControllerTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID ORGANIZATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final UUID AUDIT_LOG_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000303");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000304");
    private static final UUID SITE_ID = UUID.fromString("00000000-0000-0000-0000-000000000305");
    private static final UUID BUILDING_ID = UUID.fromString("00000000-0000-0000-0000-000000000306");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminDashboardSummaryUseCase adminDashboardSummaryUseCase;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;

    @Test
    void adminCanGetDashboardSummary() throws Exception {
        given(adminDashboardSummaryUseCase.get()).willReturn(summary());

        mockMvc.perform(
                        get("/api/v1/admin/dashboard/summary")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.recentAuditLogs[0].auditLogId")
                                .value(AUDIT_LOG_ID.toString()))
                .andExpect(jsonPath("$.data.recentAuditLogs[0].actorName").value("Admin One"))
                .andExpect(jsonPath("$.data.recentAuditLogs[0].actionType").value("USER_UPDATE"))
                .andExpect(jsonPath("$.data.recentAuditLogs[0].actorUserId").doesNotExist())
                .andExpect(jsonPath("$.data.recentAuditLogs[0].beforeSnapshot").doesNotExist())
                .andExpect(jsonPath("$.data.mailRetentionPolicy.retentionDays").value(365))
                .andExpect(jsonPath("$.data.meetingRoomSummary.todayReservationCount").value(3))
                .andExpect(jsonPath("$.data.meetingRoomSummary.inUseMeetingRoomCount").value(2))
                .andExpect(jsonPath("$.data.meetingRoomSummary.availableMeetingRoomCount").value(4))
                .andExpect(
                        jsonPath("$.data.meetingRoomSummary.timeSlotUsage[0].slotStartAt")
                                .value("2026-06-13T00:00:00Z"))
                .andExpect(
                        jsonPath("$.data.meetingRoomSummary.timeSlotOccupancyUsage[0].slotStartAt")
                                .value("2026-06-13T00:00:00Z"))
                .andExpect(
                        jsonPath("$.data.meetingRoomSummary.weekdayReservationUsage[0].dayOfWeek")
                                .value(1))
                .andExpect(
                        jsonPath(
                                        "$.data.meetingRoomSummary.weekdayReservationUsage[0].weekdayLabel")
                                .value("월"))
                .andExpect(
                        jsonPath("$.data.meetingRoomSummary.siteBuildingUsage[0].siteId")
                                .value(SITE_ID.toString()))
                .andExpect(
                        jsonPath("$.data.meetingRoomSummary.siteBuildingUsage[0].usageRate")
                                .value(0.5));
    }

    @Test
    void userCannotGetDashboardSummary() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/dashboard/summary")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));

        verifyNoInteractions(adminDashboardSummaryUseCase);
    }

    private AuthenticatedUser authenticatedUser(AuthenticatedUserRole role) {
        return new AuthenticatedUser(ADMIN_ID, ORGANIZATION_ID, role, "Admin One");
    }

    private AdminDashboardSummaryResult summary() {
        return new AdminDashboardSummaryResult(
                List.of(
                        new AdminDashboardSummaryResult.RecentAuditLogSummaryResult(
                                AUDIT_LOG_ID,
                                "Admin One",
                                "USER_UPDATE",
                                "USER",
                                TARGET_ID,
                                "SUCCESS",
                                Instant.parse("2026-06-13T01:00:00Z"))),
                new MailRetentionPolicyResult(
                        365, true, Instant.parse("2026-06-12T00:00:00Z"), ADMIN_ID),
                new AdminDashboardSummaryResult.MeetingRoomSummaryResult(
                        3,
                        2,
                        4,
                        List.of(
                                new AdminDashboardSummaryResult.TimeSlotUsageResult(
                                        Instant.parse("2026-06-13T00:00:00Z"), 1)),
                        List.of(
                                new AdminDashboardSummaryResult.TimeSlotUsageResult(
                                        Instant.parse("2026-06-13T00:00:00Z"), 2)),
                        List.of(
                                new AdminDashboardSummaryResult.WeekdayReservationUsageResult(
                                        1, "월", 3)),
                        List.of(
                                new AdminDashboardSummaryResult.SiteBuildingUsageResult(
                                        SITE_ID, "HQ", BUILDING_ID, "A", 4, 2, 0.5))));
    }
}
