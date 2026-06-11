package com.meetbowl.api.mail.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.meetbowl.application.mail.DispatchInternalMailCommand;

/**
 * 시스템 내부 메일 발송 요청 본문이다.
 *
 * <p>신뢰된 내부 호출자가 X-Internal-Token으로 호출하므로 발신자/조직 ID와 발송 본문을 직접 전달한다. 화면 인증 사용자에서 채우는 사용자 발송 요청과
 * 분리해, 내부 발송만의 입력 계약을 명확히 한다.
 */
public record InternalMailSendRequest(
        @NotNull UUID organizationId,
        @NotNull UUID senderUserId,
        @NotEmpty List<UUID> recipientUserIds,
        @NotBlank String subject,
        @NotBlank String body,
        @NotBlank String bodyType,
        String relatedResourceType,
        UUID relatedResourceId,
        @NotNull UUID idempotencyKey) {

    public DispatchInternalMailCommand toCommand() {
        return new DispatchInternalMailCommand(
                organizationId,
                senderUserId,
                recipientUserIds,
                subject,
                body,
                bodyType,
                relatedResourceType,
                relatedResourceId,
                idempotencyKey);
    }
}
