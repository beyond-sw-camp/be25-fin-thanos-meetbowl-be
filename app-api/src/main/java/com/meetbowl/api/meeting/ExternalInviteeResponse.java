package com.meetbowl.api.meeting;

import com.meetbowl.application.meeting.ExternalInviteeResult;

/** 회의 상세 응답에 포함하는 외부 초대 대상 DTO다. */
public record ExternalInviteeResponse(String name, String email) {

    public static ExternalInviteeResponse from(ExternalInviteeResult result) {
        return new ExternalInviteeResponse(result.name(), result.email());
    }
}
