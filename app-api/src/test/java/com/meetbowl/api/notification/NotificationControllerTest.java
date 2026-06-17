package com.meetbowl.api.notification;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.meetbowl.api.config.WebMvcConfig;
import com.meetbowl.application.auth.AccessTokenValidationService;
import com.meetbowl.application.notification.ListNotificationsUseCase;
import com.meetbowl.application.notification.MarkAllNotificationsReadResult;
import com.meetbowl.application.notification.MarkAllNotificationsReadUseCase;
import com.meetbowl.application.notification.MarkNotificationReadResult;
import com.meetbowl.application.notification.MarkNotificationReadUseCase;
import com.meetbowl.application.notification.NotificationPageResult;
import com.meetbowl.application.notification.NotificationResult;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CurrentUserArgumentResolver.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;
    @MockitoBean private ListNotificationsUseCase listNotificationsUseCase;
    @MockitoBean private MarkNotificationReadUseCase markNotificationReadUseCase;
    @MockitoBean private MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;
    @MockitoBean private NotificationSseService notificationSseService;

    private AuthenticatedUser user(UUID userId) {
        return new AuthenticatedUser(userId, UUID.randomUUID(), AuthenticatedUserRole.USER, "사용자");
    }

    @Test
    void listsNotificationsWithUnreadCount() throws Exception {
        UUID userId = UUID.randomUUID();
        NotificationResult item =
                new NotificationResult(
                        UUID.randomUUID(),
                        "MEETING_REMINDER",
                        "회의 시작 10분 전",
                        "회의가 곧 시작됩니다.",
                        "MEETING",
                        UUID.randomUUID(),
                        false,
                        null,
                        Instant.parse("2099-01-01T00:00:00Z"));
        when(listNotificationsUseCase.list(eq(userId), eq(1), eq(20)))
                .thenReturn(new NotificationPageResult(List.of(item), 1, 20, 1L, 1, 1L));

        mockMvc.perform(
                        get("/api/v1/notifications")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER, user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.unreadCount").value(1))
                .andExpect(jsonPath("$.data.items[0].type").value("MEETING_REMINDER"))
                .andExpect(jsonPath("$.data.items[0].read").value(false));
    }

    @Test
    void marksOneNotificationRead() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationResult read =
                new NotificationResult(
                        notificationId,
                        "MEETING_UPDATED",
                        "회의 일정 변경",
                        "일정이 변경되었습니다.",
                        "MEETING",
                        UUID.randomUUID(),
                        true,
                        Instant.parse("2099-01-01T00:00:00Z"),
                        Instant.parse("2098-12-31T00:00:00Z"));
        when(markNotificationReadUseCase.execute(notificationId, userId))
                .thenReturn(new MarkNotificationReadResult(read, 0L));

        mockMvc.perform(
                        patch("/api/v1/notifications/{id}/read", notificationId)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER, user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notification.read").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void returns404WhenNotificationMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        when(markNotificationReadUseCase.execute(notificationId, userId))
                .thenThrow(new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        mockMvc.perform(
                        patch("/api/v1/notifications/{id}/read", notificationId)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER, user(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    void returns403WhenNotificationOwnedByAnother() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        when(markNotificationReadUseCase.execute(notificationId, userId))
                .thenThrow(new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN_ACCESS));

        mockMvc.perform(
                        patch("/api/v1/notifications/{id}/read", notificationId)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER, user(userId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOTIFICATION_FORBIDDEN_ACCESS"));
    }

    @Test
    void marksAllNotificationsRead() throws Exception {
        UUID userId = UUID.randomUUID();
        when(markAllNotificationsReadUseCase.execute(userId))
                .thenReturn(new MarkAllNotificationsReadResult(3, 0L));

        mockMvc.perform(
                        patch("/api/v1/notifications/read-all")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER, user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedCount").value(3))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }
}
