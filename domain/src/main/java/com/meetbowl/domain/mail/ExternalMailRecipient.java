package com.meetbowl.domain.mail;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** 외부 메일 수신자 표시 정보다. 실제 계정이 없는 초대/안내 메일에서도 보낸함에 수신자 정보를 남기기 위해 사용한다. */
public record ExternalMailRecipient(String name, String email) {

    public ExternalMailRecipient {
        String normalizedEmail = email == null ? "" : email.trim();
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "외부 수신자 이메일 형식이 올바르지 않습니다.");
        }
        email = normalizedEmail;

        String normalizedName = name == null ? "" : name.trim();
        name = normalizedName.isBlank() ? normalizedEmail : normalizedName;
    }
}
