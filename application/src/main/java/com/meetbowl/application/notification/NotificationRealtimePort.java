package com.meetbowl.application.notification;

import java.util.UUID;

/**
 * 새 알림을 접속 중인 수신자에게 실시간(SSE)으로 밀어 주기 위한 아웃바운드 포트다.
 *
 * <p>전달은 best-effort다 — DB 저장이 원천이고, 수신자가 접속해 있지 않으면 이 포트 호출은 아무것도 하지 않으며 다음 접속 시 목록 조회로 노출된다.
 * 구현(SseEmitter 보관/전송)은 web 계층 어댑터가 담당하고, application은 "누구에게 무엇을 보낼지"만 안다.
 */
public interface NotificationRealtimePort {

    void publish(UUID recipientUserId, NotificationResult notification);
}
