package com.meetbowl.domain.mail;

import java.util.Optional;
import java.util.UUID;

/** 사용자별 메일함 상태를 메일 본문 저장소와 독립적으로 조회하고 저장하기 위한 도메인 경계다. */
public interface MailboxEntryRepositoryPort {

    MailboxEntry save(MailboxEntry mailboxEntry);

    Optional<MailboxEntry> findByMailIdAndOwnerUserId(UUID mailId, UUID ownerUserId);
}
