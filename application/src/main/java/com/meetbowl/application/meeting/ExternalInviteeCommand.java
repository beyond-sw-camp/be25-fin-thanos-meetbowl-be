package com.meetbowl.application.meeting;

/** 회의 외부 초대 입력값이다. SMTP 없이도 이름/이메일을 회의와 초대 메일 기록에 남긴다. */
public record ExternalInviteeCommand(String name, String email) {}
