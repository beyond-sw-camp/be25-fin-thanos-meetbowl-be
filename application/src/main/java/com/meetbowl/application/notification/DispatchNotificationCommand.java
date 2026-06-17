package com.meetbowl.application.notification;

import java.util.UUID;

/**
 * 시스템 내부 알림 발송 입력이다.
 *
 * <p>회의 리마인더/수정/취소, 회의록 검토 요청·지연 같은 내부 흐름이 신뢰된 토큰으로 호출한다. type/resourceType은 문자열로 받아 UseCase에서 도메인
 * enum으로 변환·검증한다(API 계층이 domain enum에 의존하지 않도록). 딥링크가 없는 알림이면 resourceType/resourceId는 둘 다 null이다.
 */
public record DispatchNotificationCommand(
        UUID recipientUserId,
        String type,
        String title,
        String content,
        String resourceType,
        UUID resourceId) {}
