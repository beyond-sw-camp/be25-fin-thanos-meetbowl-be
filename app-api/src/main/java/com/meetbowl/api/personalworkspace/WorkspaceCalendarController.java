package com.meetbowl.api.personalworkspace;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.RequireUserOrAdmin;
import com.meetbowl.api.personalworkspace.dto.CalendarEventResponse;
import com.meetbowl.api.personalworkspace.dto.CalendarSubscriptionResponse;
import com.meetbowl.api.personalworkspace.dto.CreateCalendarEventRequest;
import com.meetbowl.api.personalworkspace.dto.SubscribeCalendarRequest;
import com.meetbowl.api.personalworkspace.dto.UpdateCalendarEventRequest;
import com.meetbowl.application.personalworkspace.calendar.CalendarEventResult;
import com.meetbowl.application.personalworkspace.calendar.CalendarSubscriptionResult;
import com.meetbowl.application.personalworkspace.calendar.CreateCalendarEventCommand;
import com.meetbowl.application.personalworkspace.calendar.CreateCalendarEventUseCase;
import com.meetbowl.application.personalworkspace.calendar.DeleteCalendarEventUseCase;
import com.meetbowl.application.personalworkspace.calendar.GetCalendarSubscriptionsUseCase;
import com.meetbowl.application.personalworkspace.calendar.GetCalendarUseCase;
import com.meetbowl.application.personalworkspace.calendar.SubscribeCalendarCommand;
import com.meetbowl.application.personalworkspace.calendar.SubscribeCalendarUseCase;
import com.meetbowl.application.personalworkspace.calendar.UnsubscribeCalendarUseCase;
import com.meetbowl.application.personalworkspace.calendar.UpdateCalendarEventCommand;
import com.meetbowl.application.personalworkspace.calendar.UpdateCalendarEventUseCase;
import com.meetbowl.common.response.ApiResponse;

/**
 * 개인 워크스페이스 캘린더 API Controller다.
 *
 * <p>개인 일정 등록/수정/삭제와 동료 일정 구독/해제를 제공한다. 직접 수정·삭제할 수 있는 대상은 사용자가 만든 개인 일정으로 제한하고, 회의에서 파생된 일정은 회의
 * 흐름을 통해서만 바뀐다(여기서는 손대지 않는다).
 */
@RequireUserOrAdmin
@RestController
@RequestMapping(ApiPaths.API_V1 + "/workspace/calendar")
public class WorkspaceCalendarController extends BaseController {

    private final GetCalendarUseCase getCalendarUseCase;
    private final CreateCalendarEventUseCase createCalendarEventUseCase;
    private final UpdateCalendarEventUseCase updateCalendarEventUseCase;
    private final DeleteCalendarEventUseCase deleteCalendarEventUseCase;
    private final GetCalendarSubscriptionsUseCase getCalendarSubscriptionsUseCase;
    private final SubscribeCalendarUseCase subscribeCalendarUseCase;
    private final UnsubscribeCalendarUseCase unsubscribeCalendarUseCase;

    public WorkspaceCalendarController(
            GetCalendarUseCase getCalendarUseCase,
            CreateCalendarEventUseCase createCalendarEventUseCase,
            UpdateCalendarEventUseCase updateCalendarEventUseCase,
            DeleteCalendarEventUseCase deleteCalendarEventUseCase,
            GetCalendarSubscriptionsUseCase getCalendarSubscriptionsUseCase,
            SubscribeCalendarUseCase subscribeCalendarUseCase,
            UnsubscribeCalendarUseCase unsubscribeCalendarUseCase) {
        this.getCalendarUseCase = getCalendarUseCase;
        this.createCalendarEventUseCase = createCalendarEventUseCase;
        this.updateCalendarEventUseCase = updateCalendarEventUseCase;
        this.deleteCalendarEventUseCase = deleteCalendarEventUseCase;
        this.getCalendarSubscriptionsUseCase = getCalendarSubscriptionsUseCase;
        this.subscribeCalendarUseCase = subscribeCalendarUseCase;
        this.unsubscribeCalendarUseCase = unsubscribeCalendarUseCase;
    }

    @GetMapping
    public ApiResponse<List<CalendarEventResponse>> getCalendar(
            @CurrentUser AuthenticatedUser user,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        List<CalendarEventResult> results = getCalendarUseCase.execute(user.userId(), from, to);
        return ok(results.stream().map(CalendarEventResponse::from).toList());
    }

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> createEvent(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody CreateCalendarEventRequest request) {
        CalendarEventResult result =
                createCalendarEventUseCase.execute(
                        new CreateCalendarEventCommand(
                                user.userId(),
                                request.title(),
                                request.description(),
                                request.startedAt(),
                                request.endedAt(),
                                request.allDay()));
        return created(CalendarEventResponse.from(result));
    }

    @PatchMapping("/events/{eventId}")
    public ApiResponse<CalendarEventResponse> updateEvent(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateCalendarEventRequest request) {
        CalendarEventResult result =
                updateCalendarEventUseCase.execute(
                        new UpdateCalendarEventCommand(
                                eventId,
                                user.userId(),
                                request.title(),
                                request.description(),
                                request.startedAt(),
                                request.endedAt(),
                                request.allDay()));
        return ok(CalendarEventResponse.from(result));
    }

    @DeleteMapping("/events/{eventId}")
    public ApiResponse<Void> deleteEvent(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID eventId) {
        deleteCalendarEventUseCase.execute(user.userId(), eventId);
        return ok();
    }

    @GetMapping("/subscriptions")
    public ApiResponse<List<CalendarSubscriptionResponse>> getSubscriptions(
            @CurrentUser AuthenticatedUser user) {
        List<CalendarSubscriptionResult> results =
                getCalendarSubscriptionsUseCase.execute(user.userId());
        return ok(results.stream().map(CalendarSubscriptionResponse::from).toList());
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<ApiResponse<CalendarSubscriptionResponse>> subscribe(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody SubscribeCalendarRequest request) {
        CalendarSubscriptionResult result =
                subscribeCalendarUseCase.execute(
                        new SubscribeCalendarCommand(user.userId(), request.targetUserId()));
        return created(CalendarSubscriptionResponse.from(result));
    }

    @DeleteMapping("/subscriptions/{subscriptionId}")
    public ApiResponse<Void> unsubscribe(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID subscriptionId) {
        unsubscribeCalendarUseCase.execute(user.userId(), subscriptionId);
        return ok();
    }
}
