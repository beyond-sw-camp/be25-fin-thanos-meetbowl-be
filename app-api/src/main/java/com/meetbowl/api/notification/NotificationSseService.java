package com.meetbowl.api.notification;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.meetbowl.api.notification.dto.NotificationResponse;
import com.meetbowl.application.notification.NotificationRealtimePort;
import com.meetbowl.application.notification.NotificationResult;

/**
 * 알림 실시간 전달을 SSE로 구현하는 web 계층 어댑터다({@link NotificationRealtimePort}).
 *
 * <p>부트캠프 범위라 메시지 브로커 없이 단일 인스턴스 전제로, 수신자별 {@link SseEmitter}를 메모리에 보관한다. 한 사용자가 여러 탭/기기로 접속할 수
 * 있으므로 사용자당 emitter는 여러 개를 허용한다. 동작 원칙은 "DB가 원천, 전달은 best-effort"다 — 접속 중이면 push하고, 아니면 아무것도 하지 않으며
 * 다음 접속 시 목록 조회로 노출된다.
 *
 * <p>전송 실패(클라이언트가 끊긴 emitter)는 즉시 정리하고, 프록시/방화벽이 유휴 연결을 끊지 않도록 {@value #HEARTBEAT_MILLIS}ms마다 ping
 * 이벤트를 보낸다.
 */
@Component
public class NotificationSseService implements NotificationRealtimePort {

    /** SSE 이벤트 이름. 클라이언트는 EventSource에서 이 이름으로 리스너를 건다. */
    private static final String EVENT_NOTIFICATION = "notification";

    private static final String EVENT_PING = "ping";

    /** 하트비트 주기(30초). 합의된 기본값이며 클라이언트 재접속과 무관하게 연결만 살아 있게 한다. */
    private static final long HEARTBEAT_MILLIS = 30_000L;

    /** emitter 타임아웃. 만료되면 정리되고 클라이언트(EventSource)가 자동 재접속한다. */
    private static final long EMITTER_TIMEOUT_MILLIS = Duration.ofMinutes(30).toMillis();

    private final Map<UUID, Collection<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    /** 수신자의 SSE 구독을 등록하고 emitter를 돌려준다. 완료/타임아웃/오류 시 자동으로 목록에서 제거해 끊긴 연결이 쌓이지 않게 한다. */
    public SseEmitter subscribe(UUID recipientUserId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        Collection<SseEmitter> emitters =
                emittersByUser.computeIfAbsent(
                        recipientUserId, key -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> remove(recipientUserId, emitter));
        emitter.onTimeout(() -> remove(recipientUserId, emitter));
        emitter.onError(throwable -> remove(recipientUserId, emitter));

        // 연결 직후 한 번 보내 프록시 버퍼링으로 첫 이벤트가 지연되는 것을 막고, 구독 성공을 알린다.
        send(recipientUserId, emitter, SseEmitter.event().name(EVENT_PING).data("connected"));
        return emitter;
    }

    @Override
    public void publish(UUID recipientUserId, NotificationResult notification) {
        Collection<SseEmitter> emitters = emittersByUser.get(recipientUserId);
        if (emitters == null || emitters.isEmpty()) {
            return; // 접속 중이 아니면 전달하지 않는다 — DB에 이미 저장돼 다음 접속 시 노출된다.
        }
        NotificationResponse payload = NotificationResponse.from(notification);
        emitters.forEach(
                emitter ->
                        send(
                                recipientUserId,
                                emitter,
                                SseEmitter.event().name(EVENT_NOTIFICATION).data(payload)));
    }

    /** 유휴 연결이 끊기지 않도록 모든 emitter에 주기적으로 ping을 보낸다. 죽은 연결은 전송 실패로 감지돼 정리된다. */
    @Scheduled(fixedRate = HEARTBEAT_MILLIS)
    public void sendHeartbeat() {
        emittersByUser.forEach(
                (userId, emitters) ->
                        emitters.forEach(
                                emitter ->
                                        send(
                                                userId,
                                                emitter,
                                                SseEmitter.event()
                                                        .name(EVENT_PING)
                                                        .data("keepalive"))));
    }

    private void send(UUID userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            // 클라이언트가 이미 끊겼거나 emitter가 닫힌 경우다. 정리만 하고 전달 실패는 무시한다(best-effort).
            remove(userId, emitter);
        }
    }

    private void remove(UUID userId, SseEmitter emitter) {
        Collection<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            // 비어 있어도 computeIfAbsent 경쟁으로 새 emitter가 막 추가됐을 수 있으니, 빈 경우에만 조건부 제거한다.
            emittersByUser.remove(userId, emitters);
        }
    }
}
