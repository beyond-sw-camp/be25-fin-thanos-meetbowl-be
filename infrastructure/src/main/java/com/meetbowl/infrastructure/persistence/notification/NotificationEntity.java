package com.meetbowl.infrastructure.persistence.notification;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.meetbowl.domain.notification.Notification;
import com.meetbowl.domain.notification.NotificationResourceType;
import com.meetbowl.domain.notification.NotificationType;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 알림 JPA Entity다. {@code notification} 테이블과 1:1로 매핑된다. 수신자는 raw UUID로 참조하고, 연결 리소스(딥링크 - 알림의 해당 페이지로 가기 위한) 정보는 종류/식별자를 함께 보관한다.
 *
 * <p>목록 조회는 항상 "특정 수신자의 알림을 최신순"으로 가져오므로 {@code (recipient_user_id, created_at)} 복합 인덱스를 둔다.
 */
@Entity
@Table(
        name = "notification",
        indexes = {
            @Index(
                    name = "idx_notification_recipient_created",
                    columnList = "recipient_user_id, created_at")
        })
public class NotificationEntity extends BaseEntity {

    /** 수신자(FK, raw UUID). */
    @Column(name = "recipient_user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 연결 리소스 종류(딥링크용, 선택). */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", length = 30)
    private NotificationResourceType resourceType;

    /** 연결 리소스 식별자(딥링크용, 선택). */
    @Column(name = "resource_id", columnDefinition = "BINARY(16)")
    private UUID resourceId;

    /** 읽은 시각. null이면 미읽음. */
    @Column(name = "read_at")
    private Instant readAt;

    protected NotificationEntity() {}

    private NotificationEntity(
            UUID recipientUserId,
            NotificationType type,
            String title,
            String content,
            NotificationResourceType resourceType,
            UUID resourceId,
            Instant readAt) {
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.readAt = readAt;
    }

    static NotificationEntity from(Notification notification) {
        NotificationEntity entity =
                new NotificationEntity(
                        notification.recipientUserId(),
                        notification.type(),
                        notification.title(),
                        notification.content(),
                        notification.resourceType(),
                        notification.resourceId(),
                        notification.readAt());
        entity.setId(notification.id());
        if (notification.createdAt() != null) {
            // 재저장(읽음 처리 등)이면 기존 생성 시각을 채워, merge 후 반환 객체가 생성 시각을 잃지 않게 한다.
            entity.setCreatedAt(notification.createdAt());
        }
        return entity;
    }

    Notification toDomain() {
        return Notification.of(
                getId(),
                recipientUserId,
                type,
                title,
                content,
                resourceType,
                resourceId,
                readAt,
                getCreatedAt());
    }
}