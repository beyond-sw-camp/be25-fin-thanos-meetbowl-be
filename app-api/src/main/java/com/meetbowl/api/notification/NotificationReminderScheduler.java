package com.meetbowl.api.notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.meetbowl.application.notification.SendMeetingRemindersUseCase;
import com.meetbowl.application.notification.SendMinutesReviewRemindersUseCase;

/**
 * 시간 지연 기반 알림(MEETING_REMINDER / MINUTES_REVIEW_REMINDER)의 주기 트리거다.
 *
 * <p>이벤트 기반 알림(수정/취소/검토 요청)은 발생 시점에 내부 발송 엔드포인트가 즉시 보내지만, 이 둘은 "미래 특정 시각에 도래"하므로 주기적으로 발송 대상을 조회해
 * 보낸다. web 계층은 트리거만 담당하고, 발송 대상 판단·발송은 application UseCase가 수행한다(SSE 하트비트와 같은 구성).
 *
 * <p>{@code fixedDelay}라 이전 실행이 끝난 뒤에만 다음 실행이 돌아 주기 간 겹침이 없다. 한 주기에서 예외가 나도 Spring 스케줄러가 기록 후 다음 주기에
 * 재시도한다 — 발송은 알림 테이블을 원장으로 중복을 막으므로 재시도가 안전하다.
 */
@Component
public class NotificationReminderScheduler {

    /** 회의 리마인더 점검 주기(60초). 발송 시각을 분 단위로 맞추기 위해 분 단위로 점검한다. */
    private static final long MEETING_REMINDER_INTERVAL_MILLIS = 60_000L;

    /** 회의록 검토 재알림 점검 주기(60초). 재알림 주기는 시간 단위지만, 도래 시점을 놓치지 않도록 분 단위로 점검한다. */
    private static final long MINUTES_REVIEW_REMINDER_INTERVAL_MILLIS = 60_000L;

    /** 기동 직후 컨텍스트가 안정된 뒤 첫 점검을 시작하기 위한 지연(30초). */
    private static final long INITIAL_DELAY_MILLIS = 30_000L;

    private final SendMeetingRemindersUseCase sendMeetingRemindersUseCase;
    private final SendMinutesReviewRemindersUseCase sendMinutesReviewRemindersUseCase;

    public NotificationReminderScheduler(
            SendMeetingRemindersUseCase sendMeetingRemindersUseCase,
            SendMinutesReviewRemindersUseCase sendMinutesReviewRemindersUseCase) {
        this.sendMeetingRemindersUseCase = sendMeetingRemindersUseCase;
        this.sendMinutesReviewRemindersUseCase = sendMinutesReviewRemindersUseCase;
    }

    @Scheduled(
            fixedDelay = MEETING_REMINDER_INTERVAL_MILLIS,
            initialDelay = INITIAL_DELAY_MILLIS)
    public void sendMeetingReminders() {
        sendMeetingRemindersUseCase.run();
    }

    @Scheduled(
            fixedDelay = MINUTES_REVIEW_REMINDER_INTERVAL_MILLIS,
            initialDelay = INITIAL_DELAY_MILLIS)
    public void sendMinutesReviewReminders() {
        sendMinutesReviewRemindersUseCase.run();
    }
}