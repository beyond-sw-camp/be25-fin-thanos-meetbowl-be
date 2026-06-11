package com.meetbowl.api.mail.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendAnnouncementRequest(
        @NotBlank(message = "메일 제목은 필수입니다.") @Size(max = 200, message = "메일 제목은 200자를 초과할 수 없습니다.")
                String subject,
        @NotBlank(message = "메일 본문은 필수입니다.")
                @Size(max = 1_000_000, message = "메일 본문은 1000000자를 초과할 수 없습니다.")
                String body,
        @NotBlank(message = "메일 본문 유형은 필수입니다.")
                @Pattern(
                        regexp = "TEXT|MINUTES_SHARE",
                        message = "메일 본문 유형은 TEXT 또는 MINUTES_SHARE여야 합니다.")
                String bodyType,
        @NotNull(message = "멱등성 키는 필수입니다.") UUID idempotencyKey) {}
