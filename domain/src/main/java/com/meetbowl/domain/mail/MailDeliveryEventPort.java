package com.meetbowl.domain.mail;

/**
 * application이 내부 메일 발송 결과 이벤트 발행을 위임하는 출력 포트다.
 *
 * <p>RabbitMQ 같은 전송 기술은 infrastructure가 구현한다. 발송 완료와 실패를 한 경계로 묶어, 발송 결과를 알리는 책임이 한곳에서 일관되게 관리되도록
 * 한다.
 */
public interface MailDeliveryEventPort {

    void publishSent(MailSentEvent event);

    void publishFailed(MailFailedEvent event);
}
