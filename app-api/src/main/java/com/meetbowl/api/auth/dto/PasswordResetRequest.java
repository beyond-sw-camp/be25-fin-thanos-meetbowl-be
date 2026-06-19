package com.meetbowl.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
        @NotBlank(message = "로그인 ID는 필수입니다.") String loginId,
        @NotBlank(message = "이메일은 필수입니다.") String email) {}
