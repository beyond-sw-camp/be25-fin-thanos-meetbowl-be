package com.meetbowl.api.sharedworkspace.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 전 직원 공유 대상 설정 요청 DTO다. openToOrganization이 true면 전 직원 공개, false면 멤버 전용으로 전환한다. 의도치 않은 공개를 막기 위해
 * 값을 생략할 수 없도록 NotNull로 강제한다.
 */
public record ChangeSharedWorkspaceAudienceRequest(
        @NotNull(message = "전 직원 공유 여부는 필수입니다.") Boolean openToOrganization) {}
