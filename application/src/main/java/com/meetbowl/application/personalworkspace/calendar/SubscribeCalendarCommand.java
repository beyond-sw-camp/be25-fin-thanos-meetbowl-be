package com.meetbowl.application.personalworkspace.calendar;

import java.util.UUID;

/** 동료 일정 구독 등록 UseCase 입력이다. 구독자는 인증 사용자에서 채우고 대상 동료 ID를 받는다. */
public record SubscribeCalendarCommand(UUID subscriberUserId, UUID targetUserId) {}
