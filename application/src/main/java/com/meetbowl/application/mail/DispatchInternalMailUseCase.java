package com.meetbowl.application.mail;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailBodyType;
import com.meetbowl.domain.mail.MailDeliveryEventPort;
import com.meetbowl.domain.mail.MailFailedEvent;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailSentEvent;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.mail.RelatedResourceType;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

/**
 * 시스템 내부 메일 발송 요청을 처리하는 UseCase다.
 *
 * <p>회의록 공유 같은 내부 흐름이 신뢰된 토큰으로 호출하는 동기 진입점이다. 발송 본문을 그대로 받아 SYSTEM 유형 메일로 저장하고, 발신자/수신자 메일함 항목을 같은
 * 트랜잭션에서 만든 뒤 발송 완료를 알린다. 발송 성공은 `mail.sent`, 발송 처리 중 장애는 `mail.failed`로 알려 소비자가 재처리 여부를 판단하게 한다.
 *
 * <p>멱등성 키로 이미 발송된 요청은 같은 결과를 돌려주고 결과 이벤트를 다시 발행하지 않아, 재시도 호출이 중복 발송이나 중복 알림을 만들지 않게 한다.
 */
@Service
public class DispatchInternalMailUseCase {

    private final MailRepositoryPort mailRepositoryPort;
    private final MailboxEntryRepositoryPort mailboxEntryRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final MailDeliveryEventPort mailDeliveryEventPort;
    private final Clock clock;

    @Autowired
    public DispatchInternalMailUseCase(
            MailRepositoryPort mailRepositoryPort,
            MailboxEntryRepositoryPort mailboxEntryRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            MailDeliveryEventPort mailDeliveryEventPort) {
        this(
                mailRepositoryPort,
                mailboxEntryRepositoryPort,
                userRepositoryPort,
                mailDeliveryEventPort,
                Clock.systemUTC());
    }

    DispatchInternalMailUseCase(
            MailRepositoryPort mailRepositoryPort,
            MailboxEntryRepositoryPort mailboxEntryRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            MailDeliveryEventPort mailDeliveryEventPort,
            Clock clock) {
        this.mailRepositoryPort = mailRepositoryPort;
        this.mailboxEntryRepositoryPort = mailboxEntryRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.mailDeliveryEventPort = mailDeliveryEventPort;
        this.clock = clock;
    }

    @Transactional
    public SendMailResult execute(DispatchInternalMailCommand command) {
        var existing = mailRepositoryPort.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            // 이미 처리된 요청은 동일성만 확인하고 결과 이벤트를 다시 발행하지 않는다.
            return resolveIdempotentRequest(existing.get(), command);
        }

        Instant now = Instant.now(clock);
        // 수신자 검증 실패는 잘못된 요청이므로 발송 실패 이벤트로 알리지 않고 그대로 거절한다.
        validateRecipients(command, now);
        return dispatch(command, now);
    }

    private SendMailResult dispatch(DispatchInternalMailCommand command, Instant now) {
        // 발송 결과 이벤트가 호출자에게 돌려준 ID와 일치하도록 영속 전에 메일 ID를 발급한다.
        UUID mailId = UUID.randomUUID();
        Mail mail =
                Mail.createSystemDraft(
                        mailId,
                        command.organizationId(),
                        command.senderUserId(),
                        command.recipientUserIds(),
                        command.subject(),
                        command.body(),
                        parseBodyType(command.bodyType()),
                        parseRelatedResourceType(command.relatedResourceType()),
                        command.relatedResourceId(),
                        command.idempotencyKey());
        mail.requestDelivery(now);

        Mail saved;
        try {
            mail.markSent(now);
            saved = mailRepositoryPort.save(mail);

            List<MailboxEntry> entries = new ArrayList<>();
            entries.add(MailboxEntry.sent(saved.id(), command.senderUserId()));
            command.recipientUserIds()
                    .forEach(
                            recipientId ->
                                    entries.add(MailboxEntry.inbox(saved.id(), recipientId)));
            mailboxEntryRepositoryPort.saveAll(entries);
        } catch (RuntimeException exception) {
            // 검증을 통과한 뒤의 저장 단계 장애만 발송 실패로 보고 재처리 가능하도록 알린다.
            mailDeliveryEventPort.publishFailed(
                    new MailFailedEvent(
                            mailId,
                            command.organizationId(),
                            command.recipientUserIds(),
                            ErrorCode.MAIL_SEND_FAILED.code(),
                            ErrorCode.MAIL_SEND_FAILED.message(),
                            now,
                            true));
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED);
        }

        mailDeliveryEventPort.publishSent(
                new MailSentEvent(
                        saved.id(), saved.organizationId(), saved.recipientUserIds(), now));
        return new SendMailResult(saved.id(), saved.deliveryStatus().name(), saved.requestedAt());
    }

    private void validateRecipients(DispatchInternalMailCommand command, Instant now) {
        if (command.organizationId() == null) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "조직 정보가 없는 내부 메일은 발송할 수 없습니다.");
        }
        if (command.recipientUserIds().contains(command.senderUserId())) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "발신자를 수신자로 지정할 수 없습니다.");
        }
        // 발신자는 시스템 계정일 수 있으므로 조직 소속만 확인하고, 수신자는 같은 조직의 활성 사용자로 제한한다.
        validateSender(command.senderUserId(), command.organizationId());
        command.recipientUserIds()
                .forEach(userId -> validateRecipient(userId, command.organizationId(), now));
    }

    private void validateSender(UUID senderUserId, UUID organizationId) {
        User sender =
                userRepositoryPort
                        .findById(senderUserId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!organizationId.equals(sender.affiliateId())) {
            throw new BusinessException(
                    ErrorCode.MAIL_FORBIDDEN_ACCESS, "다른 조직의 발신자로 메일을 보낼 수 없습니다.");
        }
    }

    private void validateRecipient(UUID userId, UUID organizationId, Instant now) {
        User recipient =
                userRepositoryPort
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!organizationId.equals(recipient.affiliateId())
                || !recipient.canLoginAt(now)
                || recipient.isSystemAccount()) {
            throw new BusinessException(ErrorCode.MAIL_FORBIDDEN_ACCESS, "메일을 받을 수 없는 사용자입니다.");
        }
    }

    private SendMailResult resolveIdempotentRequest(
            Mail existing, DispatchInternalMailCommand command) {
        boolean sameRequest =
                existing.organizationId().equals(command.organizationId())
                        && existing.senderUserId().equals(command.senderUserId())
                        && existing.recipientUserIds().equals(command.recipientUserIds())
                        && existing.subject().equals(command.subject())
                        && existing.body().equals(command.body())
                        && existing.bodyType() == parseBodyType(command.bodyType())
                        && Objects.equals(
                                existing.relatedResourceType(),
                                parseRelatedResourceType(command.relatedResourceType()))
                        && Objects.equals(
                                existing.relatedResourceId(), command.relatedResourceId());
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
