package com.meetbowl.application.mail;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;

/**
 * 휴지통에 있는 내 메일을 받은/보낸 메일함으로 복구한다.
 *
 * <p>휴지통이 아닌 항목을 복구 요청하면 충돌로 거절해, 상태 전이가 휴지통→복구 순서로만 일어나도록 보장한다.
 */
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
