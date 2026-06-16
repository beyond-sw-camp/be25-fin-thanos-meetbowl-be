package com.meetbowl.infrastructure.persistence.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.meetbowl.domain.notification.NotificationType;

/**
 * 알림 엔티티의 Spring Data JPA 저장소다.
 *
 * <p>조회는 모두 "특정 수신자"를 기준으로 하며, 안 읽은 알림은 {@code readAt is null}로 구분한다. 정렬은 호출부에서 {@link Pageable}/{@link
 * Sort}로 지정한다. 패키지 외부로 노출할 필요가 없어 가시성을 패키지 전용으로 둔다.
 */
interface SpringDataNotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByRecipientUserId(UUID recipientUserId, Sort sort);

    List<NotificationEntity> findByRecipientUserId(UUID recipientUserId, Pageable pageable);

    List<NotificationEntity> findByRecipientUserIdAndReadAtIsNull(UUID recipientUserId, Sort sort);

    long countByRecipientUserId(UUID recipientUserId);

    long countByRecipientUserIdAndReadAtIsNull(UUID recipientUserId);

    List<NotificationEntity> findByType(NotificationType type);

    Optional<NotificationEntity> findFirstByRecipientUserIdAndTypeAndResourceIdOrderByCreatedAtDesc(
            UUID recipientUserId, NotificationType type, UUID resourceId);
}