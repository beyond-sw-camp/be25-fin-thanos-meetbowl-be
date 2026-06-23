package com.meetbowl.api.sharedworkspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 공유 워크스페이스 생성 요청 DTO다. 조직과 소유자는 인증 context에서 채우므로 본문에는 받지 않는다. */
public record CreateSharedWorkspaceRequest(
        @NotBlank(message = "공유 워크스페이스 이름은 필수입니다.")
                @Size(max = 100, message = "공유 워크스페이스 이름은 100자 이하여야 합니다.")
                String name,
        @Size(max = 1000, message = "공유 워크스페이스 설명은 1000자 이하여야 합니다.") String description) {}
