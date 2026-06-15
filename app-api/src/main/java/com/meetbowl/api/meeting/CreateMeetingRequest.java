package com.meetbowl.api.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 회의 생성(예약) 요청 DTO다. {@code meetingRoomId}가 있으면 물리 회의실을 점유하므로 시간대 겹침 검사를 거치고, null이면 화상회의만 진행한다.
 * 주최자는 인증 토큰에서 가져오므로 본문에 받지 않으며 자동으로 HOST 참석자가 된다. {@code attendeeUserIds}는 초대할 참석자다(최소 1명 필수). 예정
 * 시작/종료의 선후 검증은 도메인에서 수행한다.
 *
 * <p>{@code reviewerUserId}는 회의록 검토자(필수)다. 반드시 {@code attendeeUserIds}(참석자) 중 한 명이어야 하며, 해당 참석자는
 * REVIEWER 역할로 저장된다(따라서 회의에는 검토자가 될 참석자가 최소 1명 있어야 한다). 참석자 검색(계열사/부서/팀)은 조직 도메인 ElasticSearch
 * 검색(F5)에서 userId를 받아 채운다.
 */
public record CreateMeetingRequest(
        @NotBlank(message = "회의 제목은 필수입니다.") String title,
        @NotNull(message = "예정 시작 시각은 필수입니다.") Instant scheduledAt,
        @NotNull(message = "예정 종료 시각은 필수입니다.") Instant scheduledEndAt,
        UUID meetingRoomId,
        String provider,
        String providerRoomId,
        @NotEmpty(message = "참여자는 최소 1명 이상 선택해야 합니다.") List<UUID> attendeeUserIds,
        @NotNull(message = "회의록 검토자는 필수입니다.") UUID reviewerUserId,
        String description) {}
