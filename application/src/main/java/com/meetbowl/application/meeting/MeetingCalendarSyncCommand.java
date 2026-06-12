package com.meetbowl.application.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 회의 일정을 개인 워크스페이스 캘린더에 투영하기 위한 회의 모듈의 출력 계약(전달 데이터)이다.
 *
 * <p>필드 매핑 주의(회의 도메인 → 캘린더):
 *
 * <ul>
 *   <li>{@code meetingId}: 일정의 출처 식별자. 캘린더 구현체는 이 값으로 일정을 찾아 멱등 upsert/삭제를 수행한다.
 *   <li>{@code attendeeUserIds}: 이 회의의 전체 참석자(HOST/PARTICIPANT/REVIEWER) userId. 각 사용자 개인 캘린더에 일정을 투영한다.
 *   <li>{@code title}: 회의 제목.
 *   <li>{@code description}: 회의 도메인에 설명 필드가 없어 현재 항상 {@code null}로 전달된다(추후 회의에 설명이 추가되면 채움).
 *   <li>{@code startedAt}/{@code endedAt}: 회의 <b>예정</b> 시각(scheduledAt/scheduledEndAt)이다. 실제 시작/종료(런타임) 시각이
 *       아니다.
 * </ul>
 */
public record MeetingCalendarSyncCommand(
        UUID meetingId,
        List<UUID> attendeeUserIds,
        String title,
        String description,
        Instant startedAt,
        Instant endedAt) {}
