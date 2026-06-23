package com.meetbowl.infrastructure.persistence.mail;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.mail.MailboxType;

/**
 * 사용자별 메일함 항목의 {@link MailboxEntryRepositoryPort}를 JPA로 구현한다.
 *
 * <p>받은/보낸/휴지통 조회는 모두 영구 삭제 항목을 제외하고, 받은·보낸 목록은 휴지통 항목까지 제외한다. 이 제외 조건을 쿼리 메서드 이름에 담아 목록·검색이 같은 가시성
 * 규칙을 따르게 한다. 정렬은 최신 항목 우선이며, offset/limit를 페이지 번호로 환산해 위임한다.
 */
@Repository
public class JpaMailboxEntryRepositoryAdapter implements MailboxEntryRepositoryPort {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final SpringDataMailboxEntryRepository repository;

    public JpaMailboxEntryRepositoryAdapter(SpringDataMailboxEntryRepository repository) {
        this.repository = repository;
    }

    @Override
    public MailboxEntry save(MailboxEntry mailboxEntry) {
        return repository.save(MailboxEntryEntity.from(mailboxEntry)).toDomain();
    }

    @Override
    public List<MailboxEntry> saveAll(List<MailboxEntry> mailboxEntries) {
        List<MailboxEntryEntity> entities =
                mailboxEntries.stream().map(MailboxEntryEntity::from).toList();
        return repository.saveAll(entities).stream().map(MailboxEntryEntity::toDomain).toList();
    }

    @Override
    public Optional<MailboxEntry> findAccessibleByMailIdAndOwnerUserId(
            UUID mailId, UUID ownerUserId) {
        return repository
                .findByMailIdAndOwnerUserIdAndPermanentlyDeletedAtIsNullOrderByMailboxTypeAsc(
                        mailId, ownerUserId)
                .stream()
                .findFirst()
                .map(MailboxEntryEntity::toDomain);
    }

    @Override
    public List<MailboxEntry> findPageByOwnerUserIdAndMailboxType(
            UUID ownerUserId, MailboxType mailboxType, int offset, int limit) {
        return repository
                .findByOwnerUserIdAndMailboxTypeAndTrashedAtIsNullAndPermanentlyDeletedAtIsNull(
                        ownerUserId, mailboxType, page(offset, limit))
                .stream()
                .map(MailboxEntryEntity::toDomain)
                .toList();
    }

    @Override
    public long countByOwnerUserIdAndMailboxType(UUID ownerUserId, MailboxType mailboxType) {
        return repository
                .countByOwnerUserIdAndMailboxTypeAndTrashedAtIsNullAndPermanentlyDeletedAtIsNull(
                        ownerUserId, mailboxType);
    }

    @Override
    public List<MailboxEntry> findTrashPageByOwnerUserId(UUID ownerUserId, int offset, int limit) {
        return repository
                .findByOwnerUserIdAndTrashedAtIsNotNullAndPermanentlyDeletedAtIsNull(
                        ownerUserId, page(offset, limit))
                .stream()
                .map(MailboxEntryEntity::toDomain)
                .toList();
    }

    @Override
    public long countTrashByOwnerUserId(UUID ownerUserId) {
        return repository.countByOwnerUserIdAndTrashedAtIsNotNullAndPermanentlyDeletedAtIsNull(
                ownerUserId);
    }

    @Override
    public List<MailboxEntry> searchPageByOwnerUserId(
            UUID ownerUserId, String keyword, int offset, int limit) {
        return repository.searchByOwnerUserId(ownerUserId, keyword, page(offset, limit)).stream()
                .map(MailboxEntryEntity::toDomain)
                .toList();
    }

    @Override
    public long countSearchByOwnerUserId(UUID ownerUserId, String keyword) {
        return repository.countSearchByOwnerUserId(ownerUserId, keyword);
    }

    @Override
    public List<MailboxEntry> findActiveEntriesCreatedBefore(
            MailboxType mailboxType, Instant cutoff) {
        return repository
                .findByMailboxTypeAndTrashedAtIsNullAndPermanentlyDeletedAtIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(
                        mailboxType, cutoff)
                .stream()
                .map(MailboxEntryEntity::toDomain)
                .toList();
    }

    @Override
    public List<MailboxEntry> findTrashEntriesTrashedBefore(Instant cutoff) {
        return repository
                .findByTrashedAtIsNotNullAndTrashedAtBeforeAndPermanentlyDeletedAtIsNullOrderByTrashedAtAsc(
                        cutoff)
                .stream()
                .map(MailboxEntryEntity::toDomain)
                .toList();
    }

    private PageRequest page(int offset, int limit) {
        return PageRequest.of(offset / limit, limit, LATEST_FIRST);
    }
}
