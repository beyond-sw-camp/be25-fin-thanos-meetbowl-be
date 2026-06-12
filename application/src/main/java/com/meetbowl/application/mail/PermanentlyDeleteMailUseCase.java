package com.meetbowl.application.mail;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;

/**
 * 휴지통에 있는 내 메일함 항목을 영구 삭제 상태로 표시한다.
 *
 * <p>공용 메일 본문은 다른 수신자도 참조하므로 지우지 않고, 내 메일함 항목에만 삭제 시각을 남기는 soft delete로 처리한다. 휴지통 단계를 거치지 않은 영구 삭제는
 * 도메인(MailboxEntry)에서 막는다.
 */
@Service
public class PermanentlyDeleteMailUseCase {

    private final MailboxEntryRepositoryPort repository;
    private final Clock clock;

    @Autowired
    public PermanentlyDeleteMailUseCase(MailboxEntryRepositoryPort repository) {
        this(repository, Clock.systemUTC());
    }

    PermanentlyDeleteMailUseCase(MailboxEntryRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void execute(UUID mailId, UUID ownerUserId) {
        MailboxEntry entry = MailUseCaseSupport.findOwnedEntry(repository, mailId, ownerUserId);
        entry.permanentlyDelete(Instant.now(clock));
        repository.save(entry);
    }
}
