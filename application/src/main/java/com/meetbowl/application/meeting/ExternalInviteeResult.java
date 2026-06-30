package com.meetbowl.application.meeting;

import com.meetbowl.domain.meeting.MeetingExternalInvitee;

/** 회의 상세 조회에 포함하는 외부 초대 대상 정보다. */
public record ExternalInviteeResult(String name, String email) {

    public static ExternalInviteeResult from(MeetingExternalInvitee invitee) {
        return new ExternalInviteeResult(invitee.name(), invitee.email());
    }
}
