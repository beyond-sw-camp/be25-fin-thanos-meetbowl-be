package com.meetbowl.application.mail;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
import com.meetbowl.domain.mail.RelatedResourceType;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

/**
 * 사용자가 같은 조직 구성원에게 보내는 일반 메일(NORMAL) 발송을 처리한다.
 *
 * <p>발신자/수신자가 같은 조직의 활성 사용자인지 매 발송마다 검증하고, 발신자의 보낸함과 수신자의 받은함 항목을 같은 트랜잭션에서 만든다. 같은 멱등성 키의 동일 요청은
 * 기존 결과를 돌려주고, 내용이 다르면 충돌로 거절해 재시도 호출이 중복 발송을 만들지 않게 한다.
 */
@Service
public class SendMailUseCase {

    private final MailRepositoryPort mailRepositoryPort;
    private final MailboxEntryRepositoryPort mailboxEntryRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final Clock clock;

    @Autowired
    public SendMailUseCase(
            MailRepositoryPort mailRepositoryPort,
            MailboxEntryRepositoryPort mailboxEntryRepositoryPort,
            UserRepositoryPort userRepositoryPort) {
        this(mailRepositoryPort, mailboxEntryRepositoryPort, userRepositoryPort, Clock.systemUTC());
    }

    SendMailUseCase(
            MailRepositoryPort mailRepositoryPort,
            MailboxEntryRepositoryPort mailboxEntryRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            Clock clock) {
        this.mailRepositoryPort = mailRepositoryPort;
        this.mailboxEntryRepositoryPort = mailboxEntryRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.clock = clock;
    }

    @Transactional
    public SendMailResult execute(SendMailCommand command) {
        var existing = mailRepositoryPort.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return resolveIdempotentRequest(existing.get(), command);
        }

        Instant now = Instant.now(clock);
        validateParticipants(command, now);
        return createMail(command, now);
    }

    private SendMailResult createMail(SendMailCommand command, Instant now) {
        Mail mail =
                Mail.createDraft(
                        command.organizationId(),
                        command.senderUserId(),
                        command.recipientUserIds(),
                        command.subject(),
                        command.body(),
                        MailType.NORMAL,
                        parseBodyType(command.bodyType()),
                        parseRelatedResourceType(command.relatedResourceType()),
                        command.relatedResourceId(),
                        command.idempotencyKey());
        mail.requestDelivery(now);
        mail.markSent(now);
        Mail saved = mailRepositoryPort.save(mail);

        List<MailboxEntry> entries = new ArrayList<>();
        entries.add(MailboxEntry.sent(saved.id(), command.senderUserId()));
        command.recipientUserIds()
                .forEach(recipientId -> entries.add(MailboxEntry.inbox(saved.id(), recipientId)));
        mailboxEntryRepositoryPort.saveAll(entries);
        return result(saved);
    }

    private void validateParticipants(SendMailCommand command, Instant now) {
        if (command.organizationId() == null) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "조직에 소속된 사용자만 메일을 발송할 수 있습니다.");
        }
        if (command.recipientUserIds().contains(command.senderUserId())) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "발신자를 수신자로 지정할 수 없습니다.");
        }
        validateParticipant(command.senderUserId(), command.organizationId(), now);
        command.recipientUserIds()
                .forEach(userId -> validateParticipant(userId, command.organizationId(), now));
    }

    private void validateParticipant(
            java.util.UUID userId, java.util.UUID organizationId, Instant now) {
        User user =
                userRepositoryPort
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!organizationId.equals(user.affiliateId())
                || !user.canLoginAt(now)
                || user.isSystemAccount()) {
            throw new BusinessException(ErrorCode.MAIL_FORBIDDEN_ACCESS, "메일을 발송할 수 없는 사용자입니다.");
        }
    }

    private SendMailResult resolveIdempotentRequest(Mail existing, SendMailCommand command) {
        boolean sameRequest =
                existing.organizationId().equals(command.organizationId())
                        && existing.senderUserId().equals(command.senderUserId())
                        && existing.recipientUserIds().equals(command.recipientUserIds())
                        && existing.subject().equals(command.subject())
                        && existing.body().equals(command.body())
                        && existing.bodyType() == parseBodyType(command.bodyType())
                        && java.util.Objects.equals(
                                existing.relatedResourceType(),
                                parseRelatedResourceType(command.relatedResourceType()))
                        && java.util.Objects.equals(
                                existing.relatedResourceId(), command.relatedResourceId());
        if (!sameRequest) {
            throw new BusinessException(ErrorCode.MAIL_IDEMPOTENCY_CONFLICT);
        }
        return result(existing);
    }

    private SendMailResult result(Mail mail) {
        return new SendMailResult(mail.id(), mail.deliveryStatus().name(), mail.requestedAt());
    }

    private MailBodyType parseBodyType(String value) {
        try {
            return MailBodyType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "지원하지 않는 메일 본문 유형입니다.");
        }
    }

    private RelatedResourceType parseRelatedResourceType(String value) {
        if (value == null) {
            return null;
        }
        try {
            return RelatedResourceType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "지원하지 않는 관련 리소스 유형입니다.");
        }
    }
}
