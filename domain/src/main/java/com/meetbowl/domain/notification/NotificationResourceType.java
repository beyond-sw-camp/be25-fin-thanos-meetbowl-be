package com.meetbowl.domain.notification;

/**
 * 알림이 가리키는 관련 업무 리소스의 종류다. 알림 클릭 시 원본 회의/회의록으로 이동(딥링크)할 수 있게 한다.
 *
 * <p>mail의 {@code RelatedResourceType}과 의미는 유사하나, 기능 간 직접 참조를 피하기 위해 알림 전용으로 따로 둔다.
 */
public enum NotificationResourceType {
    MEETING,
    MEETING_MINUTES,
    MAIL,
    COMMUNITY_POST
}
