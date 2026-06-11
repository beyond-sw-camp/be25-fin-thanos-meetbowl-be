package com.meetbowl.api.mail.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.mail.SendMailResult;

public record SendMailResponse(UUID mailId, String deliveryStatus, Instant requestedAt) {

    public static SendMailResponse from(SendMailResult result) {
        return new SendMailResponse(result.mailId(), result.deliveryStatus(), result.requestedAt());
    }
}
