package com.meetbowl.domain.meeting;

import java.time.Instant;
import java.util.UUID;

/**
 * 회의 종료 트랜잭션과 함께 Outbox에 저장되는 도메인 이벤트다.
 *
 * <p>{@code eventId}는 RabbitMQ 재발행에서도 유지한다. 발행 성공 직후 프로세스가 중단돼 같은 이벤트가 다시 전달되더라도 consumer가 이 ID로
 * 중복을 제거할 수 있다.
 */
public record MeetingEndedEvent(
        UUID eventId,
        UUID correlationId,
        UUID meetingId,
        UUID organizationId,
        UUID hostUserId,
        UUID reviewerUserId,
        String title,
        Instant startedAt,
        Instant endedAt,
        Instant occurredAt,
        int publishAttempts) {}
