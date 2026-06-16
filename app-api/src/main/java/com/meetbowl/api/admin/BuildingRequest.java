package com.meetbowl.api.admin;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 건물 등록/수정 요청 DTO다(F1). */
public record BuildingRequest(
        @NotNull(message = "소속 사이트는 필수입니다.") UUID siteId,
        @NotBlank(message = "건물명은 필수입니다.") String name) {}
