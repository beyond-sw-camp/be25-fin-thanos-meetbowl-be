package com.meetbowl.api.minutes;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 승인된 회의록을 회의 미참석자에게 수동 공유하는 요청이다. */
public record ShareMinutesRequest(
        @NotEmpty(message = "수신자는 한 명 이상이어야 합니다.") List<@NotNull UUID> recipientUserIds,
        @NotBlank(message = "메일 제목은 필수입니다.") @Size(max = 200, message = "메일 제목은 200자를 초과할 수 없습니다.")
                String subject,
        @NotBlank(message = "메일 본문은 필수입니다.")
                @Size(max = 1_000_000, message = "메일 본문은 1000000자를 초과할 수 없습니다.")
                String body,
        @NotNull(message = "멱등성 키는 필수입니다.") UUID idempotencyKey) {}
