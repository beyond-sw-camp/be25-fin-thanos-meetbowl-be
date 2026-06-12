package com.meetbowl.api.admin;

import jakarta.validation.constraints.NotBlank;

/** 사이트+건물 동시 등록 요청 DTO다(F1 보강). 한 폼에서 입력한 사이트명·건물명을 함께 받는다. */
public record SiteWithBuildingRequest(
        @NotBlank(message = "사이트명은 필수입니다.") String siteName,
        @NotBlank(message = "건물명은 필수입니다.") String buildingName) {}
