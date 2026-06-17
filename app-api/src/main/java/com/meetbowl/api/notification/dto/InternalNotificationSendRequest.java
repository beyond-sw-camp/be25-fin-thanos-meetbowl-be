package com.meetbowl.api.notification.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.meetbowl.application.notification.DispatchNotificationCommand;

/**
 * 시스템 내부 알림 발송 요청 본문이다.
 *
 * <p>회의 리마인더/수정/취소, 회의록 검토 요청·지연 같은 내부 흐름이 X-Internal-Token으로 호출한다. type/resourceType은 문자열로 받아
 * UseCase에서 검증하며(잘못된 값은 400), 딥링크가 없는 알림이면 resourceType/resourceId는 둘 다 비워 보낸다.
 */
public record InternalNotificationSendRequest(
        @NotNull UUID recipientUserId,
        @NotBlank String type,
        @NotBlank String title,
        @NotBlank String content,
        String resourceType,
        UUID resourceId) {

    // API 요청 DTO를 UseCase 입력으로 그대로 넘기지 않도록, application 계약으로 변환하는 경계다.
    public DispatchNotificationCommand toCommand() {
        return new DispatchNotificationCommand(
                recipientUserId, type, title, content, resourceType, resourceId);
    }
}
