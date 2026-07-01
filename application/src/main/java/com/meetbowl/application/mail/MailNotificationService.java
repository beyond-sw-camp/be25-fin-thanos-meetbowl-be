package com.meetbowl.application.mail;

import org.springframework.stereotype.Service;

import com.meetbowl.application.notification.DispatchNotificationCommand;
import com.meetbowl.application.notification.DispatchNotificationUseCase;
import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailBodyType;
import com.meetbowl.domain.mail.RelatedResourceType;
import com.meetbowl.domain.notification.NotificationResourceType;
import com.meetbowl.domain.notification.NotificationType;

/**
 * 메일 발송 결과를 사용자 알림으로 연결하는 application 서비스다.
 *
 * <p>메일 저장과 수신자 메일함 생성이 끝난 뒤 같은 트랜잭션에서 알림 행을 생성한다. 실시간 SSE 전달은
 * {@link DispatchNotificationUseCase}가 커밋 이후에 처리하므로, 메일 저장이 롤백되면 알림도 함께 롤백된다.
 */
@Service
public class MailNotificationService {

    private final DispatchNotificationUseCase dispatchNotificationUseCase;

    public MailNotificationService(DispatchNotificationUseCase dispatchNotificationUseCase) {
        this.dispatchNotificationUseCase = dispatchNotificationUseCase;
    }

    public void notifyRecipients(Mail mail) {
        NotificationType type = notificationType(mail);
        String title = title(type, mail);
        String content = content(type, mail);
        mail.recipientUserIds()
                .forEach(
                        recipientUserId ->
                                dispatchNotificationUseCase.execute(
                                        new DispatchNotificationCommand(
                                                recipientUserId,
                                                type.name(),
                                                title,
                                                content,
                                                NotificationResourceType.MAIL.name(),
                                                mail.id())));
    }

    private NotificationType notificationType(Mail mail) {
        if (mail.bodyType() == MailBodyType.MINUTES_SHARE
                || mail.relatedResourceType() == RelatedResourceType.MEETING_MINUTES) {
            return NotificationType.MAIL_SHARED;
        }
        return NotificationType.MAIL_RECEIVED;
    }

    private String title(NotificationType type, Mail mail) {
        if (type == NotificationType.MAIL_SHARED) {
            return "메일로 자료가 공유되었습니다";
        }
        return "새 메일이 도착했습니다";
    }

    private String content(NotificationType type, Mail mail) {
        if (type == NotificationType.MAIL_SHARED) {
            return "공유 메일을 확인해 주세요: " + mail.subject();
        }
        return "받은 메일함을 확인해 주세요: " + mail.subject();
    }
}
