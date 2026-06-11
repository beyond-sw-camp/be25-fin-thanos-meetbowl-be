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

    /**
     * 사용자가 소유한 메일함 항목 중 제목 또는 본문에 키워드를 포함한 항목을 검색한다. 휴지통과 영구 삭제 항목은 검색 대상에서 제외해 받은/보낸 메일함 화면과 동일한
     * 가시성 기준을 유지한다.
     */
    List<MailboxEntry> searchPageByOwnerUserId(
            UUID ownerUserId, String keyword, int offset, int limit);

    long countSearchByOwnerUserId(UUID ownerUserId, String keyword);
}
