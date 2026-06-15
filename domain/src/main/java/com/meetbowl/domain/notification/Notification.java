package com.meetbowl.domain.notification;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 단방향 알림 도메인 모델이다(FR-022, FR-126). 수신자 한 명당 한 행으로 저장하며 읽음 상태를 각자 보유한다.
 *
 * <p> 알림은 DB에 먼저 저장되고, 수신자가 접속 중이면 SSE로 즉시 push, 접속 중이 아니면 저장만 되어 다음 접속 시 목록 조회로 노출된다.
 * 따라서 이 모델은 전달(SSE) 여부를 알지 못하며 보관하지 않는다 — 전달은 best-effort, DB가 기준이다.
 *
 * <p>{@code resourceType}/{@code resourceId}는 알림 클릭 시 원본 회의/회의록으로 이동시키기 위한 딥링크 정보이며, 둘은 항상 함께 존재하거나 함께
 * 비어 있어야 한다(부분 지정 금지). 읽음 처리는 {@link #markRead(Instant)}로 수행하고, 첫 읽은 시각을 보존하기 위해 이미 읽은 알림은 변경하지 않는다.
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

    private Notification(
            UUID id,
            UUID recipientUserId,
            NotificationType type,
            String title,
            String content,
            NotificationResourceType resourceType,
            UUID resourceId,
            Instant readAt) {
        this.id = id;
        this.recipientUserId = requireNonNull(recipientUserId, "수신자는 필수입니다.");
        this.type = requireNonNull(type, "알림 종류는 필수입니다.");
        this.title = requireText(title, "알림 제목은 필수입니다.");
        this.content = requireText(content, "알림 내용은 필수입니다.");
        validateResource(resourceType, resourceId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.readAt = readAt;
    }

    /** 새 알림 생성(미읽음 상태). */
    public static Notification create(
            UUID recipientUserId,
            NotificationType type,
            String title,
            String content,
            NotificationResourceType resourceType,
            UUID resourceId) {
        return new Notification(
                null, recipientUserId, type, title, content, resourceType, resourceId, null);
    }

    /** DB 복원용. 저장된 id/읽음 시각을 그대로 채워 재구성한다. */
    public static Notification of(
            UUID id,
            UUID recipientUserId,
            NotificationType type,
            String title,
            String content,
            NotificationResourceType resourceType,
            UUID resourceId,
            Instant readAt) {
        return new Notification(
                id, recipientUserId, type, title, content, resourceType, resourceId, readAt);
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