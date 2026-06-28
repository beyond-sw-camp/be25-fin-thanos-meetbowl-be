package com.meetbowl.api.meeting;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import com.meetbowl.application.meeting.ExternalInviteeCommand;

/** 회의 외부 초대 대상 입력 DTO다. */
public record ExternalInviteeRequest(
        @Size(max = 120, message = "외부 초대 이름은 120자 이하여야 합니다.") String name,
        @Email(message = "외부 초대 이메일 형식이 올바르지 않습니다.")
                @Size(max = 255, message = "외부 초대 이메일은 255자 이하여야 합니다.")
                String email) {

    public ExternalInviteeCommand toCommand() {
        return new ExternalInviteeCommand(name, email);
    }
}
