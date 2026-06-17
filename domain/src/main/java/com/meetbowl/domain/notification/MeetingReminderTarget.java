package com.meetbowl.domain.notification;

import java.time.Instant;
import java.util.UUID;

/**
 * 회의 시작 전 리마인더(MEETING_REMINDER) 발송에 필요한 회의 최소 정보 투영이다.
 *
 * <p>알림 스케줄러는 회의 도메인 전체가 아니라 "언제 시작하는 어떤 회의인지"만 알면 되므로, 회의 본체({@code Meeting})를 그대로 끌어오지 않고 이 작은 읽기
 * 모델만 가져온다. {@code scheduledAt}에서 각 참석자의 개인 알림 시간(분)을 빼 발송 시각을 계산하고, {@code title}은 알림 본문에 쓴다.
 */
public record MeetingReminderTarget(UUID meetingId, String title, Instant scheduledAt) {}
