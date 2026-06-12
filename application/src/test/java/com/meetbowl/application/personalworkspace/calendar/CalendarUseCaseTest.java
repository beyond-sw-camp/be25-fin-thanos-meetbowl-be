package com.meetbowl.application.personalworkspace.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEvent;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEventRepositoryPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarSubscriptionRepositoryPort;

/** 개인 캘린더 UseCase의 기간 검증, 소유자 조회, 중복 구독 차단 같은 분기 규칙을 검증한다. */
class CalendarUseCaseTest {

    private final PersonalWorkspaceCalendarEventRepositoryPort eventPort =
            Mockito.mock(PersonalWorkspaceCalendarEventRepositoryPort.class);
    private final PersonalWorkspaceCalendarSubscriptionRepositoryPort subscriptionPort =
            Mockito.mock(PersonalWorkspaceCalendarSubscriptionRepositoryPort.class);

    @Test
    void getCalendar_fail_when_period_inverted() {
        GetCalendarUseCase useCase = new GetCalendarUseCase(eventPort);
        Instant from = Instant.parse("2026-06-10T10:00:00Z");
        Instant to = Instant.parse("2026-06-10T09:00:00Z");

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(UUID.randomUUID(), from, to));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, ex.errorCode());
        verify(eventPort, never()).findVisibleByUserIdAndPeriod(any(), any(), any());
    }

    @Test
    void getCalendar_success_returns_visible_events() {
        GetCalendarUseCase useCase = new GetCalendarUseCase(eventPort);
        UUID userId = UUID.randomUUID();
        Instant from = Instant.parse("2026-06-10T00:00:00Z");
        Instant to = Instant.parse("2026-06-11T00:00:00Z");
        PersonalWorkspaceCalendarEvent event =
                PersonalWorkspaceCalendarEvent.createPersonal(
                        userId, "회고", null, from.plusSeconds(3600), from.plusSeconds(7200), false);
        when(eventPort.findVisibleByUserIdAndPeriod(userId, from, to)).thenReturn(List.of(event));

        List<CalendarEventResult> results = useCase.execute(userId, from, to);

        assertEquals(1, results.size());
        assertEquals("회고", results.get(0).title());
        assertEquals("PERSONAL", results.get(0).source());
    }

    @Test
    void updateEvent_fail_when_not_found() {
        UpdateCalendarEventUseCase useCase = new UpdateCalendarEventUseCase(eventPort);
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(eventPort.findByIdAndOwnerUserId(eventId, userId)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new UpdateCalendarEventCommand(
                                                eventId,
                                                userId,
                                                "수정",
                                                null,
                                                Instant.parse("2026-06-10T10:00:00Z"),
                                                Instant.parse("2026-06-10T11:00:00Z"),
                                                false)));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, ex.errorCode());
    }

    @Test
    void deleteEvent_fail_when_nothing_deleted() {
        DeleteCalendarEventUseCase useCase = new DeleteCalendarEventUseCase(eventPort);
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(eventPort.deletePersonalByIdAndOwnerUserId(eventId, userId)).thenReturn(false);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> useCase.execute(userId, eventId));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, ex.errorCode());
    }

    @Test
    void subscribe_fail_when_already_subscribed() {
        SubscribeCalendarUseCase useCase = new SubscribeCalendarUseCase(subscriptionPort);
        UUID subscriber = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        when(subscriptionPort.existsBySubscriberUserIdAndTargetUserId(subscriber, target))
                .thenReturn(true);

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(new SubscribeCalendarCommand(subscriber, target)));

        assertEquals(ErrorCode.COMMON_CONFLICT, ex.errorCode());
        verify(subscriptionPort, never()).save(any());
    }

    @Test
    void subscribe_success_when_not_subscribed() {
        SubscribeCalendarUseCase useCase = new SubscribeCalendarUseCase(subscriptionPort);
        UUID subscriber = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        when(subscriptionPort.existsBySubscriberUserIdAndTargetUserId(subscriber, target))
                .thenReturn(false);
        when(subscriptionPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CalendarSubscriptionResult result =
                useCase.execute(new SubscribeCalendarCommand(subscriber, target));

        assertEquals(target, result.targetUserId());
        assertTrue(result.subscribedAt() != null);
        verify(subscriptionPort).save(any());
    }
}
