package com.meetbowl.api.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MyPasswordChangeRequest(
        @NotBlank(message = "현재 비밀번호는 필수입니다.") String currentPassword,
        @NotBlank(message = "새 비밀번호는 필수입니다.")
                @Size(min = 8, max = 100, message = "새 비밀번호는 8자 이상 100자 이하여야 합니다.")
                String newPassword,
        @NotBlank(message = "새 비밀번호 확인은 필수입니다.") String newPasswordConfirm) {

    @AssertTrue(message = "새 비밀번호와 새 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordConfirmed() {
        return newPassword != null && newPassword.equals(newPasswordConfirm);
    }
}
