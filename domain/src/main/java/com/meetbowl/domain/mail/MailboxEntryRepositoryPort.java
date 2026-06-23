package com.meetbowl.domain.mail;

import java.time.Instant;
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

    /**
     * 보관 기간이 지난 받은/보낸 메일함 항목을 자동 휴지통 이동 대상으로 조회한다.
     *
     * <p>휴지통 또는 영구 삭제 상태는 제외한다. 자동 삭제 배치는 수동 삭제와 같은 도메인 상태 전이를 사용해야 하므로, 벌크 update 대신 도메인 객체 목록을
     * 반환한다. 대량 메일함에서 한 번에 너무 많은 행을 점유하지 않도록 호출자는 limit으로 처리 묶음 크기를 제한한다.
     */
    List<MailboxEntry> findActiveEntriesCreatedBefore(
            MailboxType mailboxType, Instant cutoff, int limit);

    /**
     * 휴지통 보관 기간이 지난 항목을 영구 삭제 대상으로 조회한다.
     *
     * <p>이미 영구 삭제된 항목은 제외한다. 영구 삭제는 다른 사용자의 메일 본문을 지우지 않고 현재 소유자의 메일함 항목에만 삭제 시각을 남긴다.
     */
    List<MailboxEntry> findTrashEntriesTrashedBefore(Instant cutoff, int limit);
}
