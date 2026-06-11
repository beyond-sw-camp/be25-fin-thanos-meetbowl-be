package com.meetbowl.infrastructure.persistence.mail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.mail.MailboxType;

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

    private PageRequest page(int offset, int limit) {
        return PageRequest.of(offset / limit, limit, LATEST_FIRST);
    }
}
