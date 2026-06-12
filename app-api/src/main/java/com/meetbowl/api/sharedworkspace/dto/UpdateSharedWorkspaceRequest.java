package com.meetbowl.api.sharedworkspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 공유 워크스페이스 정보 수정 요청 DTO다. */
public record UpdateSharedWorkspaceRequest(
        @NotBlank(message = "공유 워크스페이스 이름은 필수입니다.")
                @Size(max = 100, message = "공유 워크스페이스 이름은 100자 이하여야 합니다.")
                String name,
        @Size(max = 1000, message = "공유 워크스페이스 설명은 1000자 이하여야 합니다.") String description) {}
