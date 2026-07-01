package com.meetbowl.domain.notification;

/**
 * 알림 종류다. 대부분 api-spec의 내부 발송 엔드포인트(§14)와 대응한다.
 *
 * <p>발송 트리거는 각각 다음과 같다.
 *
 * <ul>
 *   <li>{@link #MEETING_REMINDER} — 회의 시작 전, 개인 설정의 알림 시간(분)에 맞춰 참석자에게 발송.
 *   <li>{@link #MEETING_UPDATED} — 회의 일정 수정 시 기존 참석자와 새로 추가된 참석자에게 발송.
 *   <li>{@link #MEETING_CANCELLED} — 회의 취소(DELETE /meetings/{id}) 시 참석자에게 발송. 내부 발송 엔드포인트는 api-spec
 *       §14에 아직 없어 추가가 필요하다(PR 리뷰 시 합의 후 보강).
 *   <li>{@link #MINUTES_REVIEW_REQUEST} — AI 회의록 1차 요약 완료 시 지정된 검토자에게 검토 요청.
 *   <li>{@link #MINUTES_REVIEW_REMINDER} — 검토자가 기한 내 검토하지 않을 때 보내는 추가(재)알림.
 *   <li>{@link #MAIL_RECEIVED} — 일반/공지/내부 메일이 수신자의 받은함에 도착했을 때 발송.
 *   <li>{@link #MAIL_SHARED} — 회의록 승인 등으로 메일을 통해 자료가 공유됐을 때 발송.
 * </ul>
 */
public enum NotificationType {
    MEETING_REMINDER,
    MEETING_UPDATED,
    MEETING_CANCELLED,
    MINUTES_REVIEW_REQUEST,
    MINUTES_REVIEW_REMINDER,
    MAIL_RECEIVED,
    MAIL_SHARED
}
