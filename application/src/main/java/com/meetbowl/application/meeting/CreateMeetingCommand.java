package com.meetbowl.application.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 회의 생성(예약) 입력 모델이다. {@code meetingRoomId}가 있으면 물리 회의실을 점유하므로 시간대 겹침 검사를 거치고, null이면 화상회의만 진행한다.
 * {@code hostUserId}는 인증된 요청자(주최자)를 app-api에서 채워 전달한다. {@code attendeeUserIds}는 초대할 참석자이며, 주최자는 자동으로
 * HOST 참석자로 포함되므로 목록에 없어도 된다.
 *
 * <p>{@code reviewerUserId}는 회의록 검토자다. 검토자는 반드시 {@code attendeeUserIds}(참석자) 중 한 명이어야 하며, 지정 시 해당
 * 참석자는 REVIEWER 역할로 저장된다. null이면 검토자 없이 생성한다.
 *
 * <p>경계: 참석자/검토자는 유저(조직) 도메인 사용자 테이블에서 선택된 사용자다. 계열사/부서/팀 기준 참석자 검색은 ElasticSearch 기반 조직 도메인
 * 검색(F5)에서 처리하므로, 본 입력 모델은 이미 확정된 userId만 담는다(사용자 존재/유효성 검증은 조직 도메인 책임).
 */
public record CreateMeetingCommand(
        String title,
        Instant scheduledAt,
        Instant scheduledEndAt,
        UUID hostUserId,
        UUID meetingRoomId,
        String provider,
        String providerRoomId,
        List<UUID> attendeeUserIds,
        UUID reviewerUserId,
        String description) {}
