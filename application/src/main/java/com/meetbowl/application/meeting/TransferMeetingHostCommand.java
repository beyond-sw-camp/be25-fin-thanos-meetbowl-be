package com.meetbowl.application.meeting;

import java.util.UUID;

/** 회의 관리자를 다른 참석자로 이전하기 위한 요청이다. */
public record TransferMeetingHostCommand(
        UUID meetingId, UUID requesterUserId, UUID newHostUserId) {}
