package com.meetbowl.application.meeting;

import java.time.Instant;
import java.util.UUID;

/**
 * 회의 종료 요청이다.
 *
 * <p>실제 회의 종료는 호스트 버튼, STT 세션 stop, 장애 복구 재처리 등 여러 경로에서 들어올 수 있으므로 요청 주체와 사유를 함께 받는다.
 */
public record EndMeetingCommand(
        UUID meetingId, Instant endedAt, UUID correlationId, String reason, String triggeredBy) {}
