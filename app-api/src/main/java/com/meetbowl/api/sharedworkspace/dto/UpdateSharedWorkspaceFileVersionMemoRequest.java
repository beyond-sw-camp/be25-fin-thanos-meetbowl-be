package com.meetbowl.api.sharedworkspace.dto;

import jakarta.validation.constraints.Size;

/** 버전 변경 메모 수정 요청 DTO다. 메모를 비우는 것도 허용하므로 NotBlank를 두지 않고 길이만 제한한다. */
public record UpdateSharedWorkspaceFileVersionMemoRequest(
        @Size(max = 1000, message = "변경 메모는 1000자 이하여야 합니다.") String changeMemo) {}
