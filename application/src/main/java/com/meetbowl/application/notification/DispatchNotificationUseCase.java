package com.meetbowl.application.notification;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.notification.Notification;
import com.meetbowl.domain.notification.NotificationRepositoryPort;
import com.meetbowl.domain.notification.NotificationResourceType;
import com.meetbowl.domain.notification.NotificationType;

/**
 * 시스템 내부 알림 발송을 처리하는 UseCase다.
 *
 * <p>알림을 DB에 먼저 저장하고, 트랜잭션 커밋이 성공한 뒤에만 접속 중인 수신자에게 SSE로 push한다. 커밋 전에 push하면 롤백 시 존재하지 않는
 * 알림을 보내게 되므로, {@link TransactionSynchronization#afterCommit()}로 미뤄 정합성을 지킨다. push는 best-effort라 실패해도
 * 발송 자체는 성공으로 본다.
 */
@Service
public class DispatchNotificationUseCase {

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final NotificationRealtimePort notificationRealtimePort;

    public DispatchNotificationUseCase(
            NotificationRepositoryPort notificationRepositoryPort,
            NotificationRealtimePort notificationRealtimePort) {
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.notificationRealtimePort = notificationRealtimePort;
    }

    @Transactional
    public NotificationResult execute(DispatchNotificationCommand command) {
        Notification notification =
                Notification.create(
                        command.recipientUserId(),
                        parseType(command.type()),
                        command.title(),
                        command.content(),
                        parseResourceType(command.resourceType()),
                        command.resourceId());

        Notification saved = notificationRepositoryPort.save(notification);
        NotificationResult result = NotificationResult.from(saved);
        publishAfterCommit(saved.recipientUserId(), result);
        return result;
    }

    /** 커밋 이후에만 실시간 전달을 시도한다. 트랜잭션이 없으면(테스트 등) 즉시 전달한다. */
    private void publishAfterCommit(UUID recipientUserId, NotificationResult result) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationRealtimePort.publish(recipientUserId, result);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationRealtimePort.publish(recipientUserId, result);
                    }
                });
    }

    private NotificationType parseType(String value) {
        try {
            return NotificationType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "지원하지 않는 알림 종류입니다.");
        }
    }

    private NotificationResourceType parseResourceType(String value) {
        if (value == null) {
            return null;
        }
        try {
            return NotificationResourceType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "지원하지 않는 연결 리소스 종류입니다.");
        }
    }
}