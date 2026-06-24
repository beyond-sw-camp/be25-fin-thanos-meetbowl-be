package com.meetbowl.api.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 참석자 시간 겹침 실시간 검사 요청 DTO다. 회의 생성/수정 폼에서 선택한 참석자({@code userIds})와 예정 시간대를 보내 겹침 여부를 조회한다. {@code
 * excludeMeetingId}는 수정 중인 회의 자신을 겹침 검사에서 제외할 때 쓴다(생성 시 null). 예정 시작/종료의 선후는 단순 조회라 도메인 저장처럼 강제하지
 * 않는다.
 */
public record AttendeeAvailabilityRequest(
        @NotEmpty(message = "확인할 참석자는 최소 1명 이상이어야 합니다.") List<UUID> userIds,
        @NotNull(message = "예정 시작 시각은 필수입니다.") Instant scheduledAt,
        @NotNull(message = "예정 종료 시각은 필수입니다.") Instant scheduledEndAt,
        UUID excludeMeetingId) {}
