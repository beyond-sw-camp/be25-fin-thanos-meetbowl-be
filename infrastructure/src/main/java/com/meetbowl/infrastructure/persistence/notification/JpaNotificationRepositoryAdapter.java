package com.meetbowl.infrastructure.persistence.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.meetbowl.domain.notification.Notification;
import com.meetbowl.domain.notification.NotificationRepositoryPort;
import com.meetbowl.domain.notification.NotificationType;

/**
 * 알림 저장소 포트({@link NotificationRepositoryPort})를 JPA로 구현한다.
 *
 * <p>알림은 항상 최신순으로 노출되므로 모든 목록 조회에 생성 시각 내림차순 정렬을 적용한다. 페이지 조회는 offset/limit를 페이지 번호로 환산해 위임한다. 도메인
 * 모델과 엔티티 간 변환만 담당하고, 가시성 규칙(수신자 본인 한정)이나 페이지 번호 환산 같은 정책은 호출하는 UseCase가 맡는다.
 */
@Repository
public class JpaNotificationRepositoryAdapter implements NotificationRepositoryPort {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final SpringDataNotificationRepository repository;

    public JpaNotificationRepositoryAdapter(SpringDataNotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Notification save(Notification notification) {
        return repository.save(NotificationEntity.from(notification)).toDomain();
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return repository.findById(id).map(NotificationEntity::toDomain);
    }

    @Override
    public List<Notification> findByRecipientUserId(UUID recipientUserId) {
        return repository.findByRecipientUserId(recipientUserId, LATEST_FIRST).stream()
                .map(NotificationEntity::toDomain)
                .toList();
    }

    @Override
    public List<Notification> findUnreadByRecipientUserId(UUID recipientUserId) {
        return repository
                .findByRecipientUserIdAndReadAtIsNull(recipientUserId, LATEST_FIRST)
                .stream()
                .map(NotificationEntity::toDomain)
                .toList();
    }

    @Override
    public List<Notification> findPageByRecipientUserId(
            UUID recipientUserId, int offset, int limit) {
        return repository.findByRecipientUserId(recipientUserId, page(offset, limit)).stream()
                .map(NotificationEntity::toDomain)
                .toList();
    }

    @Override
    public long countByRecipientUserId(UUID recipientUserId) {
        return repository.countByRecipientUserId(recipientUserId);
    }

    @Override
    public long countUnreadByRecipientUserId(UUID recipientUserId) {
        return repository.countByRecipientUserIdAndReadAtIsNull(recipientUserId);
    }

    @Override
    public List<Notification> findByType(NotificationType type) {
        return repository.findByType(type).stream().map(NotificationEntity::toDomain).toList();
    }

    @Override
    public Optional<Notification> findLatestByRecipientUserIdAndTypeAndResourceId(
            UUID recipientUserId, NotificationType type, UUID resourceId) {
        return repository
                .findFirstByRecipientUserIdAndTypeAndResourceIdOrderByCreatedAtDesc(
                        recipientUserId, type, resourceId)
                .map(NotificationEntity::toDomain);
    }

    private PageRequest page(int offset, int limit) {
        return PageRequest.of(offset / limit, limit, LATEST_FIRST);
    }
}