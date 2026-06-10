package com.meetbowl.application.minutes;

import java.util.UUID;

/** 회의록 수정 UseCase 입력이다. 인증 사용자와 HTTP 요청 값을 분리해 application 계층에 전달한다. */
public record ReviseMinutesCommand(
        UUID meetingId, UUID actorUserId, UUID actorOrganizationId, String summary) {}
