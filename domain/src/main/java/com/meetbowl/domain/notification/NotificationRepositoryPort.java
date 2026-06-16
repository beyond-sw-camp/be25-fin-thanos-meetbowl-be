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

    /**
     * 수신자의 알림을 최신순으로 한 페이지만 조회한다. offset/limit 기반이며, page 번호 환산은 호출자(UseCase)가 맡는다. 목록 API가 전체를 메모리에
     * 올리지 않도록 하기 위한 페이지 조회 진입점이다.
     */
    List<Notification> findPageByRecipientUserId(UUID recipientUserId, int offset, int limit);

    /** 수신자의 전체 알림 개수. 목록 응답의 totalElements/totalPages 계산용. */
    long countByRecipientUserId(UUID recipientUserId);

    /** 수신자의 안 읽은 알림 개수. 목록·읽음 처리 응답의 unreadCount 계산용. */
    long countUnreadByRecipientUserId(UUID recipientUserId);

    /**
     * 특정 종류의 알림을 모두 조회한다. 재알림 스케줄러가 발송 기준이 되는 원본 알림을 찾는 데 사용한다(예:
     * {@code MINUTES_REVIEW_REQUEST}를 검토 요청 시각의 원장으로 재사용 ).
     *
     * <p>수신자 제약이 없는 전역 조회라 종류별 양이 누적되면 비용이 커진다. 현재 규모에선 충분하나, 추후 발송 대상을 좁히려면
     * 미읽음/기간 조건을 가진 조회로 분화하는 것을 고려한다.
     */
    List<Notification> findByType(NotificationType type);

    /**
     * 한 수신자에 대해 같은 종류·같은 연결 리소스를 가진 알림 중 가장 최근 한 건을 조회한다. 재알림 스케줄러에서
     * (1) 동일 대상 재알림이 이미 나갔는지(중복 방지)와 (2) 마지막 발송 시각 기준 다음 주기 도래 판단에 사용한다.
     */
    Optional<Notification> findLatestByRecipientUserIdAndTypeAndResourceId(
            UUID recipientUserId, NotificationType type, UUID resourceId);
}