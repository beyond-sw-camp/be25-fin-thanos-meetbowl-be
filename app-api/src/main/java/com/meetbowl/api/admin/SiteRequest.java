package com.meetbowl.api.admin;

import jakarta.validation.constraints.NotBlank;

/** 사이트 등록/수정 요청 DTO다(F1). */
public record SiteRequest(@NotBlank(message = "사이트명은 필수입니다.") String name, String address) {}
