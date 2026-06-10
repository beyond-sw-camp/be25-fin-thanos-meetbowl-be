package com.meetbowl.domain.mail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 사용자별 메일함 상태를 메일 본문 저장소와 독립적으로 조회하고 저장하기 위한 도메인 경계다. */
public interface MailboxEntryRepositoryPort {

    MailboxEntry save(MailboxEntry mailboxEntry);

    List<MailboxEntry> saveAll(List<MailboxEntry> mailboxEntries);

    Optional<MailboxEntry> findAccessibleByMailIdAndOwnerUserId(UUID mailId, UUID ownerUserId);

    List<MailboxEntry> findPageByOwnerUserIdAndMailboxType(
            UUID ownerUserId, MailboxType mailboxType, int offset, int limit);

    long countByOwnerUserIdAndMailboxType(UUID ownerUserId, MailboxType mailboxType);

    List<MailboxEntry> findTrashPageByOwnerUserId(UUID ownerUserId, int offset, int limit);

    long countTrashByOwnerUserId(UUID ownerUserId);
}
