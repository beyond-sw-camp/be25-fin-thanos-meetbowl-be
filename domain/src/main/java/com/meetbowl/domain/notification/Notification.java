package com.meetbowl.domain.notification;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 단방향 알림 도메인 모델이다(FR-022, FR-126). 수신자 한 명당 한 행으로 저장하며 읽음 상태를 각자 보유한다.
 *
 * <p>알림은 DB에 먼저 저장되고, 수신자가 접속 중이면 SSE로 즉시 push, 접속 중이 아니면 저장만 되어 다음 접속 시 목록 조회로 노출된다. 따라서 이 모델은
 * 전달(SSE) 여부를 알지 못하며 보관하지 않는다 — 전달은 best-effort, DB가 기준이다.
 *
 * <p>{@code resourceType}/{@code resourceId}는 알림 클릭 시 원본 회의/회의록으로 이동시키기 위한 딥링크 정보이며, 둘은 항상 함께 존재하거나
 * 함께 비어 있어야 한다(부분 지정 금지). 읽음 처리는 {@link #markRead(Instant)}로 수행하고, 첫 읽은 시각을 보존하기 위해 이미 읽은 알림은 변경하지
 * 않는다.
 */
public class Notification {

    private final UUID id;

    /** 수신자(소유자). 본인만 조회/읽음 처리할 수 있다. */
    private final UUID recipientUserId;

    private final NotificationType type;

    /** 알림 제목(목록 표시용). */
    private final String title;

    /** 알림 본문(상세 표시용). */
    private final String content;

    /** 연결 리소스 종류(딥링크용, 선택). */
    private final NotificationResourceType resourceType;

    /** 연결 리소스 식별자(딥링크용, 선택). */
    private final UUID resourceId;

    /** 읽은 시각. null이면 아직 안 읽음. */
    private Instant readAt;

    /**
     * 생성 시각(목록 최신순 정렬·표시용). 실제 값은 저장 시 JPA 감사(@CreatedDate)가 채우므로, 새로 만든 알림(create)에서는 null이고 DB에서
     * 복원한 알림(of)에서만 채워진다 — {@link #id}와 같은 재구성 전용 필드다.
     *
     * <p>{@code MINUTES_REVIEW_REQUEST}의 경우 재알림 스케줄러가 이 값을 '검토 요청 시각'으로 재사용한다. 변경/제거 시 주의 — 알림 생성
     * 시각과 검토 요청 시각이 분리되면 재알림 기준시각이 어긋난다.
     */
    private final Instant createdAt;

    private Notification(
            UUID id,
            UUID recipientUserId,
            NotificationType type,
            String title,
            String content,
            NotificationResourceType resourceType,
            UUID resourceId,
            Instant readAt,
            Instant createdAt) {
        this.id = id;
        this.recipientUserId = requireNonNull(recipientUserId, "수신자는 필수입니다.");
        this.type = requireNonNull(type, "알림 종류는 필수입니다.");
        this.title = requireText(title, "알림 제목은 필수입니다.");
        this.content = requireText(content, "알림 내용은 필수입니다.");
        validateResource(resourceType, resourceId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    /** 새 알림 생성(미읽음 상태). id/생성 시각은 저장 시점에 채워지므로 아직 비어 있다. */
    public static Notification create(
            UUID recipientUserId,
            NotificationType type,
            String title,
            String content,
            NotificationResourceType resourceType,
            UUID resourceId) {
        return new Notification(
                null, recipientUserId, type, title, content, resourceType, resourceId, null, null);
    }

    /** DB 복원용. 저장된 id/읽음 시각/생성 시각을 그대로 채워 재구성한다. */
    public static Notification of(
            UUID id,
            UUID recipientUserId,
            NotificationType type,
            String title,
            String content,
            NotificationResourceType resourceType,
            UUID resourceId,
            Instant readAt,
            Instant createdAt) {
        return new Notification(
                id,
                recipientUserId,
                type,
                title,
                content,
                resourceType,
                resourceId,
                readAt,
                createdAt);
    }

    /** 읽음 처리. 이미 읽은 알림은 첫 읽은 시각을 보존하기 위해 변경하지 않는다(read-all 시 멱등). */
    public void markRead(Instant readAt) {
        if (isRead()) {
            return;
        }
        this.readAt = requireNonNull(readAt, "읽은 시각은 필수입니다.");
    }

    public boolean isRead() {
        return readAt != null;
    }

    public boolean isOwnedBy(UUID userId) {
        return recipientUserId.equals(userId);
    }

    public UUID id() {
        return id;
    }

    public UUID recipientUserId() {
        return recipientUserId;
    }

    public NotificationType type() {
        return type;
    }

    public String title() {
        return title;
    }

    public String content() {
        return content;
    }

    public NotificationResourceType resourceType() {
        return resourceType;
    }

    public UUID resourceId() {
        return resourceId;
    }

    public Instant readAt() {
        return readAt;
    }

    /** 생성 시각. 저장 전 새 알림에서는 null이다. */
    public Instant createdAt() {
        return createdAt;
    }

    /** 딥링크 정보는 종류와 식별자가 항상 짝을 이뤄야 한다(한쪽만 지정 시 어디로 보낼지 모호해진다). */
    private static void validateResource(NotificationResourceType resourceType, UUID resourceId) {
        if ((resourceType == null) != (resourceId == null)) {
            throw invalid("연결 리소스 종류와 식별자는 함께 지정하거나 함께 비워야 합니다.");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw invalid(message);
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String message) {
        if (Objects.isNull(value)) {
            throw invalid(message);
        }
        return value;
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
    }
}
