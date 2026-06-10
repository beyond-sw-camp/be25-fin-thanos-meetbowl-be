package com.meetbowl.application.mail;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;

@Service
public class RestoreMailUseCase {

    private final MailboxEntryRepositoryPort repository;

    public RestoreMailUseCase(MailboxEntryRepositoryPort repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(UUID mailId, UUID ownerUserId) {
        MailboxEntry entry = MailUseCaseSupport.findOwnedEntry(repository, mailId, ownerUserId);
        if (!entry.isTrashed()) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "휴지통에 있는 메일만 복구할 수 있습니다.");
        }
        entry.restore();
        repository.save(entry);
    }
}
