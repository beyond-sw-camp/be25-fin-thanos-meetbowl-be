package com.meetbowl.application.mail;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailBodyType;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailType;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

/**
 * 관리자가 같은 조직 구성원 전체에게 공지 메일을 발송한다.
 *
 * <p>수신자는 요청 본문이 아니라 발신자 조직의 활성 사용자로 서버가 계산해, 권한 없는 대상에게 임의 발송하거나 발송 시점의 비활성 사용자에게 노출되는 것을 막는다. 일반
 * 메일과 동일하게 발송과 동시에 완료 상태로 저장하되, 다수 수신자를 다루는 점만 다르다.
 */
@Service
public class SendAnnouncementUseCase {

    private final MailRepositoryPort mailRepositoryPort;
    private final MailboxEntryRepositoryPort mailboxEntryRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final MailNotificationService mailNotificationService;
    private final Clock clock;

    @Autowired
    public SendAnnouncementUseCase(
            MailRepositoryPort mailRepositoryPort,
            MailboxEntryRepositoryPort mailboxEntryRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            MailNotificationService mailNotificationService) {
        this(
                mailRepositoryPort,
                mailboxEntryRepositoryPort,
                userRepositoryPort,
                mailNotificationService,
                Clock.systemUTC());
    }

    SendAnnouncementUseCase(
            MailRepositoryPort mailRepositoryPort,
            MailboxEntryRepositoryPort mailboxEntryRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            MailNotificationService mailNotificationService,
            Clock clock) {
        this.mailRepositoryPort = mailRepositoryPort;
        this.mailboxEntryRepositoryPort = mailboxEntryRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.mailNotificationService = mailNotificationService;
        this.clock = clock;
    }

    @Transactional
    public SendMailResult execute(SendAnnouncementCommand command) {
        if (command.organizationId() == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_FORBIDDEN, "조직에 소속된 관리자만 공지 메일을 발송할 수 있습니다.");
        }

        var existing = mailRepositoryPort.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return resolveIdempotentRequest(existing.get(), command);
        }

        Instant now = Instant.now(clock);
        List<UUID> recipientIds = resolveRecipients(command, now);

        Mail mail =
                Mail.createDraft(
                        command.organizationId(),
                        command.senderUserId(),
                        recipientIds,
                        List.of(),
                        command.subject(),
                        command.body(),
                        MailType.ANNOUNCEMENT,
                        parseBodyType(command.bodyType()),
                        null,
                        null,
                        command.idempotencyKey());
        mail.requestDelivery(now);
        mail.markSent(now);
        Mail saved = mailRepositoryPort.save(mail);

        List<MailboxEntry> entries = new ArrayList<>();
        entries.add(MailboxEntry.sent(saved.id(), command.senderUserId()));
        recipientIds.forEach(
                recipientId -> entries.add(MailboxEntry.inbox(saved.id(), recipientId)));
        mailboxEntryRepositoryPort.saveAll(entries);
        mailNotificationService.notifyRecipients(saved);

        return new SendMailResult(saved.id(), saved.deliveryStatus().name(), saved.requestedAt());
    }

    /** 발신자 조직의 활성 일반/관리자 계정만 수신자로 삼고, 발신자 본인과 시스템 계정은 제외한다. */
    private List<UUID> resolveRecipients(SendAnnouncementCommand command, Instant now) {
        List<UUID> recipientIds =
                userRepositoryPort.findAllByAffiliateId(command.organizationId()).stream()
                        .filter(user -> !user.id().equals(command.senderUserId()))
                        .filter(user -> !user.isSystemAccount())
                        .filter(user -> user.canLoginAt(now))
                        .map(User::id)
                        .toList();
        if (recipientIds.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "공지 메일을 받을 수 있는 사용자가 없습니다.");
        }
        return recipientIds;
    }

    private SendMailResult resolveIdempotentRequest(
            Mail existing, SendAnnouncementCommand command) {
        boolean sameRequest =
                existing.mailType() == MailType.ANNOUNCEMENT
                        && existing.organizationId().equals(command.organizationId())
                        && existing.senderUserId().equals(command.senderUserId())
                        && existing.subject().equals(command.subject())
                        && existing.body().equals(command.body())
                        && existing.bodyType() == parseBodyType(command.bodyType());
        if (!sameRequest) {
            throw new BusinessException(ErrorCode.MAIL_IDEMPOTENCY_CONFLICT);
        }
        return new SendMailResult(
                existing.id(), existing.deliveryStatus().name(), existing.requestedAt());
    }

    private MailBodyType parseBodyType(String value) {
        try {
            return MailBodyType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "지원하지 않는 메일 본문 유형입니다.");
        }
    }
}
