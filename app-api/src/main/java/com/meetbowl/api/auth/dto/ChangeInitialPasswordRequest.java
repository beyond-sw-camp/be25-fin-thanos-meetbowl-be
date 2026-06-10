package com.meetbowl.api.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeInitialPasswordRequest(
        @NotBlank(message = "새 비밀번호는 필수입니다.")
                @Size(min = 8, max = 100, message = "새 비밀번호는 8자 이상 100자 이하여야 합니다.")
                String newPassword,
        @NotBlank(message = "새 비밀번호 확인은 필수입니다.") String newPasswordConfirm) {

    @AssertTrue(message = "새 비밀번호와 확인 값이 일치하지 않습니다.")
    public boolean isPasswordConfirmed() {
        return newPassword != null && newPassword.equals(newPasswordConfirm);
    }
}
