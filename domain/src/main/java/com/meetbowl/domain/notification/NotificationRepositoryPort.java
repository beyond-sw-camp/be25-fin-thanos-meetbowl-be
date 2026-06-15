package com.meetbowl.domain.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 알림 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface NotificationRepositoryPort {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    /** 수신자의 알림 목록(최신순). GET /notifications 응답용. */
    List<Notification> findByRecipientUserId(UUID recipientUserId);

    /** 수신자의 안 읽은 알림 목록. 전체 읽음 처리(read-all) 시 대상만 골라 갱신하기 위해 사용한다. */
    List<Notification> findUnreadByRecipientUserId(UUID recipientUserId);
}