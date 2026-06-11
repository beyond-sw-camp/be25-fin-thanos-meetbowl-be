package com.meetbowl.api.mail.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/** 사용자 메일 발송 요청 본문이다. 제목/본문 길이와 본문 유형을 검증하고, 관련 리소스는 유형·ID를 함께 지정하도록 강제한다. */
public record SendMailRequest(
        @NotEmpty(message = "수신자는 한 명 이상이어야 합니다.") List<@NotNull UUID> recipientUserIds,
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
        @Pattern(regexp = "MEETING|MEETING_MINUTES|WORKSPACE", message = "관련 리소스 유형이 올바르지 않습니다.")
                String relatedResourceType,
        UUID relatedResourceId,
        @NotNull(message = "멱등성 키는 필수입니다.") UUID idempotencyKey) {

    @AssertTrue(message = "관련 리소스 유형과 ID는 함께 지정해야 합니다.")
    @Schema(hidden = true)
    public boolean isRelatedResourceComplete() {
        return (relatedResourceType == null) == (relatedResourceId == null);
    }
}
