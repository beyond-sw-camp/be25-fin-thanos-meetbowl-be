package com.meetbowl.application.meeting;

import java.time.Instant;
import java.util.UUID;

/**
 * 회의 종료 후 후속 비동기 처리를 시작시키는 이벤트 발행 Port다.
 *
 * <p>회의 상태 확정은 `meetbowl-be`가 수행하고, 그 다음 회의록 생성 같은 느린 작업은 RabbitMQ 같은 외부 메시징으로 넘긴다.
 */
public interface MeetingEndedEventPublisher {

    void publishMeetingEnded(
            UUID meetingId,
            UUID hostUserId,
            UUID reviewerUserId,
            String title,
            Instant startedAt,
            Instant endedAt,
            UUID correlationId);
}
