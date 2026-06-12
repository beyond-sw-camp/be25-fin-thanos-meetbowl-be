package com.meetbowl.application.minutes;

import java.util.UUID;

/** 회의록 승인 UseCase 입력이다. 승인 시각은 서버 UTC 시각을 사용하므로 요청에서 받지 않는다. */
public record ApproveMinutesCommand(UUID meetingId, UUID actorUserId, UUID actorOrganizationId) {}
