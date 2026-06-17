package com.meetbowl.application.notification;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;
import com.meetbowl.domain.minutes.MinutesStatus;
import com.meetbowl.domain.notification.Notification;
import com.meetbowl.domain.notification.NotificationRepositoryPort;
import com.meetbowl.domain.notification.NotificationResourceType;
import com.meetbowl.domain.notification.NotificationType;
import com.meetbowl.domain.user.UserSetting;
import com.meetbowl.domain.user.UserSettingRepositoryPort;

/**
 * 회의록 검토 미완료 재알림(MINUTES_REVIEW_REMINDER) 스케줄러.
 *
 * <p>[기준시각 설계 — notification.createdAt 재사용] 재알림은 "검토 요청 후 기한 내 미검토 시 N분 주기로 재발송"이므로 기준시각 = '검토 요청
 * 시각'이 필요하다. 그러나 Minutes 도메인에는 검토 요청 시각 필드가 없다 (IN_REVIEW로 상태 전이만 하고, approvedAt/sharedAt은 있어도
 * reviewRequestedAt은 없음).
 *
 * <p>Minutes는 회의록 팀 소유 엔티티이므로 필드를 임의 추가하지 않고, 이미 존재하는 MINUTES_REVIEW_REQUEST 알림 행의 createdAt을
 * 기준시각으로 재사용한다. 검토 요청 시점에 해당 알림이 생성되므로, 현재 로직에서 notification.createdAt(알림 생성 시각) == 검토 요청 시각 이
 * 성립한다. 즉 알림 테이블이 기준시각 원장 역할을 겸한다.
 *
 * <p>[검토했던 대안] - B. Minutes에 reviewRequestedAt 추가 → 가장 명시적이나 팀원 소유 엔티티 침범, 보류. - C. 별도 스케줄
 * 테이블(minutesId, dueAt) → 깔끔하나 테이블 추가, 현 규모엔 과함.
 *
 * <p>[전제·주의] 이 방식은 '검토 요청 시각'과 '알림 생성 시각'이 동일하다는 가정에 의존한다. 추후 둘이 분리되면(예: 검토 요청과 알림 발송 사이 지연/재전송 도입)
 * 기준시각이 어긋난다. 그 경우 Minutes.reviewRequestedAt 도입(방법 B)으로 전환한다.
 *
 * <p>TODO: Minutes 도메인에 reviewRequestedAt이 생기면 기준시각을 그쪽으로 이관.
 */
@Service
public class SendMinutesReviewRemindersUseCase {

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final MinutesRepositoryPort minutesRepositoryPort;
    private final UserSettingRepositoryPort userSettingRepositoryPort;
    private final DispatchNotificationUseCase dispatchNotificationUseCase;
    private final Clock clock;

    public SendMinutesReviewRemindersUseCase(
            NotificationRepositoryPort notificationRepositoryPort,
            MinutesRepositoryPort minutesRepositoryPort,
            UserSettingRepositoryPort userSettingRepositoryPort,
            DispatchNotificationUseCase dispatchNotificationUseCase,
            Clock clock) {
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.minutesRepositoryPort = minutesRepositoryPort;
        this.userSettingRepositoryPort = userSettingRepositoryPort;
        this.dispatchNotificationUseCase = dispatchNotificationUseCase;
        this.clock = clock;
    }

    /** 주기 호출 진입점. 이번 주기에 실제로 보낸 재알림 수를 반환한다(로깅/테스트 확인용). */
    public int run() {
        Instant now = Instant.now(clock);
        int sent = 0;
        for (ReminderCandidate candidate : findCandidates().values()) {
            if (sendIfDue(now, candidate)) {
                sent++;
            }
        }
        return sent;
    }

    /**
     * 재알림 대상 조회.
     *
     * <p>다음을 모두 만족하는 MINUTES_REVIEW_REQUEST 알림을 찾는다: 1) createdAt이 기준 시각(N분/시간) 이상 경과 ← '검토 요청
     * 시각'으로 간주 2) 해당 Minutes가 아직 IN_REVIEW (검토 미완료) 3) 동일 회의록에 대한 재알림(REMINDER)이 아직 미발송 (중복 방지)
     *
     * <p>주기 재발송이므로, 마지막 REMINDER 발송 시각 기준으로 다음 주기 도래 여부를 판단해 중복/누락을 막는다.
     *
     * <p>이 메서드는 (수신자, 회의록) 단위로 후보를 한 건씩만 모은다 — 같은 검토 요청이 재전송돼 알림 행이 여럿이어도 가장 이른 요청 시각을 기준으로 삼아 한
     * 주기에 한 번만 보내도록 한다. 도래 판단(1·3)과 발송은 {@link #sendIfDue}에서 수행한다.
     */
    private Map<ReminderKey, ReminderCandidate> findCandidates() {
        Map<ReminderKey, ReminderCandidate> candidates = new LinkedHashMap<>();
        for (Notification request :
                notificationRepositoryPort.findByType(NotificationType.MINUTES_REVIEW_REQUEST)) {
            UUID minutesId = request.resourceId();
            if (minutesId == null || !isStillInReview(minutesId)) {
                continue; // 검토 완료/삭제된 회의록은 더 이상 재촉하지 않는다.
            }
            ReminderKey key = new ReminderKey(request.recipientUserId(), minutesId);
            candidates.merge(
                    key,
                    new ReminderCandidate(
                            request.recipientUserId(), minutesId, request.createdAt()),
                    ReminderCandidate::earlierRequest);
        }
        return candidates;
    }

    private boolean sendIfDue(Instant now, ReminderCandidate candidate) {
        int periodMinutes = reminderPeriodMinutes(candidate.recipientUserId());
        Instant baseline = lastReminderAt(candidate).orElse(candidate.requestedAt());
        Instant dueAt = baseline.plus(Duration.ofMinutes(periodMinutes));
        if (now.isBefore(dueAt)) {
            return false; // 아직 다음 주기 도래 전이다.
        }
        dispatchNotificationUseCase.execute(
                new DispatchNotificationCommand(
                        candidate.recipientUserId(),
                        NotificationType.MINUTES_REVIEW_REMINDER.name(),
                        "회의록 검토 미완료 알림",
                        "검토가 완료되지 않은 회의록이 있습니다. 검토를 진행해 주세요.",
                        NotificationResourceType.MEETING_MINUTES.name(),
                        candidate.minutesId()));
        return true;
    }

    private boolean isStillInReview(UUID minutesId) {
        return minutesRepositoryPort
                .findById(minutesId)
                .map(Minutes::status)
                .filter(status -> status == MinutesStatus.IN_REVIEW)
                .isPresent();
    }

    /** 마지막으로 보낸 재알림의 생성 시각. 없으면 비어 있고, 그때는 검토 요청 시각이 기준이 된다. */
    private Optional<Instant> lastReminderAt(ReminderCandidate candidate) {
        return notificationRepositoryPort
                .findLatestByRecipientUserIdAndTypeAndResourceId(
                        candidate.recipientUserId(),
                        NotificationType.MINUTES_REVIEW_REMINDER,
                        candidate.minutesId())
                .map(Notification::createdAt);
    }

    private int reminderPeriodMinutes(UUID userId) {
        return userSettingRepositoryPort
                .findByUserId(userId)
                .map(UserSetting::minutesReviewReminderMinutes)
                .orElse(UserSetting.DEFAULT_MINUTES_REVIEW_REMINDER_MINUTES);
    }

    private record ReminderKey(UUID recipientUserId, UUID minutesId) {}

    private record ReminderCandidate(UUID recipientUserId, UUID minutesId, Instant requestedAt) {

        /** 같은 (수신자, 회의록)에 검토 요청 알림이 여럿이면 가장 이른 요청 시각을 기준으로 삼는다. */
        ReminderCandidate earlierRequest(ReminderCandidate other) {
            return other.requestedAt.isBefore(this.requestedAt) ? other : this;
        }
    }
}
