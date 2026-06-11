package com.meetbowl.domain.mail;

/**
 * application이 내부 메일 발송 결과 이벤트 발행을 위임하는 출력 포트다.
 *
 * <p>RabbitMQ 같은 전송 기술은 infrastructure가 구현한다. 발송 완료와 실패를 한 경계로 묶어, 발송 결과를 알리는 책임이 한곳에서 일관되게 관리되도록
 * 한다.
 */
public interface MailDeliveryEventPort {

    // 메일과 메일함 항목 저장이 끝나 발송이 확정된 뒤에만 호출한다.
    void publishSent(MailSentEvent event);

    // 검증을 통과한 요청이 저장 단계에서 실패했을 때 호출해 재처리 판단 근거를 남긴다.
    void publishFailed(MailFailedEvent event);
}
