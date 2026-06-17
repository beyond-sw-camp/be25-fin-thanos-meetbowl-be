package com.meetbowl.api.notification;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.meetbowl.api.common.ApiHeaders;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.RequireUserOrAdmin;
import com.meetbowl.api.notification.dto.MarkAllNotificationsReadResponse;
import com.meetbowl.api.notification.dto.MarkNotificationReadResponse;
import com.meetbowl.api.notification.dto.NotificationPageResponse;
import com.meetbowl.application.notification.ListNotificationsUseCase;
import com.meetbowl.application.notification.MarkAllNotificationsReadUseCase;
import com.meetbowl.application.notification.MarkNotificationReadUseCase;
import com.meetbowl.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * 사용자 알림 API Controller다.
 *
 * <p>목록 조회, 단건/전체 읽음 처리, 그리고 실시간 수신용 SSE 구독을 제공한다. 사용자 식별자는 본문/쿼리가 아닌 @CurrentUser에서만 채워 다른 사용자의
 * 알림을 건드리지 못하게 하고, 권한 검증은 어노테이션과 SecurityConfig가 맡는다.
 */
@Validated
@RestController
@RequireUserOrAdmin
@SecurityRequirement(name = ApiHeaders.AUTHORIZATION)
@RequestMapping(ApiPaths.API_V1 + "/notifications")
public class NotificationController extends BaseController {

    private final ListNotificationsUseCase listNotificationsUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;
    private final NotificationSseService notificationSseService;

    public NotificationController(
            ListNotificationsUseCase listNotificationsUseCase,
            MarkNotificationReadUseCase markNotificationReadUseCase,
            MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase,
            NotificationSseService notificationSseService) {
        this.listNotificationsUseCase = listNotificationsUseCase;
        this.markNotificationReadUseCase = markNotificationReadUseCase;
        this.markAllNotificationsReadUseCase = markAllNotificationsReadUseCase;
        this.notificationSseService = notificationSseService;
    }

    @GetMapping
    public ApiResponse<NotificationPageResponse> list(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ok(
                NotificationPageResponse.from(
                        listNotificationsUseCase.list(user.userId(), page, size)));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<MarkNotificationReadResponse> markRead(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @PathVariable UUID notificationId) {
        return ok(
                MarkNotificationReadResponse.from(
                        markNotificationReadUseCase.execute(notificationId, user.userId())));
    }

    @PatchMapping("/read-all")
    public ApiResponse<MarkAllNotificationsReadResponse> markAllRead(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user) {
        return ok(
                MarkAllNotificationsReadResponse.from(
                        markAllNotificationsReadUseCase.execute(user.userId())));
    }

    /**
     * 실시간 알림 수신용 SSE 구독이다.
     *
     * <p>EventSource는 커스텀 헤더(Authorization)를 붙일 수 없어, 이 엔드포인트에 한해 access token을 {@code ?token=} 쿼리
     * 파라미터로 받는다(SecurityConfig의 BearerTokenResolver 참고). 응답은 {@code text/event-stream}이라 공통 응답
     * 엔벨로프(ApiResponse)로 감싸지 않고 emitter를 그대로 반환한다.
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@Parameter(hidden = true) @CurrentUser AuthenticatedUser user) {
        return notificationSseService.subscribe(user.userId());
    }
}
