package com.meetbowl.domain.meeting;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** 회의에 초대한 외부 게스트의 표시 정보다. 내부 사용자 계정과 무관하게 이름/이메일만 저장한다. */
public record MeetingExternalInvitee(UUID id, UUID meetingId, String name, String email) {

    public static MeetingExternalInvitee create(UUID meetingId, String name, String email) {
        return of(null, meetingId, name, email);
    }

    public static MeetingExternalInvitee of(UUID id, UUID meetingId, String name, String email) {
        if (meetingId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "외부 초대 대상 회의 ID는 필수입니다.");
        }
        String normalizedEmail = email == null ? "" : email.trim();
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "외부 초대 이메일 형식이 올바르지 않습니다.");
        }
        String normalizedName = name == null ? "" : name.trim();
        return new MeetingExternalInvitee(
                id,
                meetingId,
                normalizedName.isBlank() ? normalizedEmail : normalizedName,
                normalizedEmail);
    }
}
