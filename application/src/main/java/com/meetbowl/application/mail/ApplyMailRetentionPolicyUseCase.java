package com.meetbowl.application.mail;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.mail.MailRetentionPolicy;
import com.meetbowl.domain.mail.MailRetentionPolicyRepositoryPort;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.mail.MailboxType;

/** 메일 보관 정책의 자동 삭제 옵션을 실제 메일함 상태에 적용한다. */
@Service
public class ApplyMailRetentionPolicyUseCase {

    private static final UUID SYSTEM_POLICY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final MailRetentionPolicyRepositoryPort policyRepositoryPort;
    private final MailboxEntryRepositoryPort mailboxEntryRepositoryPort;
    private final Clock clock;

    @Autowired
    public ApplyMailRetentionPolicyUseCase(
            MailRetentionPolicyRepositoryPort policyRepositoryPort,
            MailboxEntryRepositoryPort mailboxEntryRepositoryPort) {
        this(policyRepositoryPort, mailboxEntryRepositoryPort, Clock.systemUTC());
    }

    ApplyMailRetentionPolicyUseCase(
            MailRetentionPolicyRepositoryPort policyRepositoryPort,
            MailboxEntryRepositoryPort mailboxEntryRepositoryPort,
            Clock clock) {
        this.policyRepositoryPort = policyRepositoryPort;
        this.mailboxEntryRepositoryPort = mailboxEntryRepositoryPort;
        this.clock = clock;
    }

    /**
     * 현재 저장된 시스템 메일 보관 정책을 읽어 만료 항목을 정리한다.
     *
     * <p>정책 row가 없거나 {@code autoDeleteEnabled=false}이면 기존 메일함 상태를 변경하지 않는다. 받은/보낸 메일은 바로 영구 삭제하지 않고
     * 먼저 휴지통으로 이동해 사용자가 복구할 수 있는 단계를 보장한다. 휴지통 항목만 휴지통 보관 기간을 기준으로 영구 삭제 상태로 바꾼다.
     */
    @Transactional
    public MailRetentionApplyResult execute() {
        return policyRepositoryPort
                .findById(SYSTEM_POLICY_ID)
                .filter(MailRetentionPolicy::autoDeleteEnabled)
                .map(this::apply)
                .orElseGet(MailRetentionApplyResult::disabled);
    }

    private MailRetentionApplyResult apply(MailRetentionPolicy policy) {
        Instant now = Instant.now(clock);
        int inboxMovedCount =
                moveActiveEntriesToTrash(
                        MailboxType.INBOX,
                        now.minus(policy.inboxRetentionDays(), ChronoUnit.DAYS),
                        now);
        int sentMovedCount =
                moveActiveEntriesToTrash(
                        MailboxType.SENT,
                        now.minus(policy.sentRetentionDays(), ChronoUnit.DAYS),
                        now);
        int permanentlyDeletedCount =
                permanentlyDeleteTrashEntries(
                        now.minus(policy.trashRetentionDays(), ChronoUnit.DAYS), now);

        return new MailRetentionApplyResult(
                true, inboxMovedCount, sentMovedCount, permanentlyDeletedCount);
    }

    private int moveActiveEntriesToTrash(MailboxType mailboxType, Instant cutoff, Instant now) {
        List<MailboxEntry> expiredEntries =
                mailboxEntryRepositoryPort.findActiveEntriesCreatedBefore(mailboxType, cutoff);
        if (expiredEntries.isEmpty()) {
            return 0;
        }
        expiredEntries.forEach(entry -> entry.moveToTrash(now));
        mailboxEntryRepositoryPort.saveAll(expiredEntries);
        return expiredEntries.size();
    }

    private int permanentlyDeleteTrashEntries(Instant cutoff, Instant now) {
        List<MailboxEntry> expiredTrashEntries =
                mailboxEntryRepositoryPort.findTrashEntriesTrashedBefore(cutoff);
        if (expiredTrashEntries.isEmpty()) {
            return 0;
        }
        expiredTrashEntries.forEach(entry -> entry.permanentlyDelete(now));
        mailboxEntryRepositoryPort.saveAll(expiredTrashEntries);
        return expiredTrashEntries.size();
    }
}
