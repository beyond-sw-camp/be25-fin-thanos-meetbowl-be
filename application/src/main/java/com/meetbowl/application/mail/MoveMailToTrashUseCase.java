package com.meetbowl.application.mail;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;

/**
 * 현재 사용자의 메일함 항목을 휴지통으로 이동한다.
 *
 * <p>공용 메일이나 다른 수신자의 상태는 건드리지 않고 내 메일함 항목만 옮긴다. 이미 휴지통에 있는 항목을 다시 요청하면 충돌로 거절해 중복 처리를 막는다.
 */
@Service
public class MoveMailToTrashUseCase {

    private final MailboxEntryRepositoryPort repository;
    private final Clock clock;

    @Autowired
    public MoveMailToTrashUseCase(MailboxEntryRepositoryPort repository) {
        this(repository, Clock.systemUTC());
    }

    MoveMailToTrashUseCase(MailboxEntryRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void execute(UUID mailId, UUID ownerUserId) {
        MailboxEntry entry = MailUseCaseSupport.findOwnedEntry(repository, mailId, ownerUserId);
        if (entry.isTrashed()) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "이미 휴지통에 있는 메일입니다.");
        }
        entry.moveToTrash(Instant.now(clock));
        repository.save(entry);
    }
}
