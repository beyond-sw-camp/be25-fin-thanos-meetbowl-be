package com.meetbowl.api.mail.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.mail.SendMailResult;

/** 메일 발송 응답 본문이다. 발송 결과(메일 ID·상태·요청 시각)를 클라이언트 계약으로 변환해 노출한다. */
public record SendMailResponse(UUID mailId, String deliveryStatus, Instant requestedAt) {

    public static SendMailResponse from(SendMailResult result) {
        return new SendMailResponse(result.mailId(), result.deliveryStatus(), result.requestedAt());
    }
}
