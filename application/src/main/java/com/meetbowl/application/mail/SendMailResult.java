package com.meetbowl.application.mail;

import java.time.Instant;
import java.util.UUID;

/** 메일 발송 결과다. 생성된 메일 ID와 발송 상태, 요청 시각을 호출자에 돌려준다. */
public record SendMailResult(UUID mailId, String deliveryStatus, Instant requestedAt) {}
