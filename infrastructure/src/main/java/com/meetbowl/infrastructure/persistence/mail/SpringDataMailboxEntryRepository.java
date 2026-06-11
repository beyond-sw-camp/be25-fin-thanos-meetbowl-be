package com.meetbowl.infrastructure.persistence.mail;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.meetbowl.domain.mail.MailboxType;

interface SpringDataMailboxEntryRepository extends JpaRepository<MailboxEntryEntity, UUID> {

    List<MailboxEntryEntity>
            findByMailIdAndOwnerUserIdAndPermanentlyDeletedAtIsNullOrderByMailboxTypeAsc(
                    UUID mailId, UUID ownerUserId);

    List<MailboxEntryEntity>
            findByOwnerUserIdAndMailboxTypeAndTrashedAtIsNullAndPermanentlyDeletedAtIsNull(
                    UUID ownerUserId, MailboxType mailboxType, Pageable pageable);

    long countByOwnerUserIdAndMailboxTypeAndTrashedAtIsNullAndPermanentlyDeletedAtIsNull(
            UUID ownerUserId, MailboxType mailboxType);

    List<MailboxEntryEntity> findByOwnerUserIdAndTrashedAtIsNotNullAndPermanentlyDeletedAtIsNull(
            UUID ownerUserId, Pageable pageable);

    long countByOwnerUserIdAndTrashedAtIsNotNullAndPermanentlyDeletedAtIsNull(UUID ownerUserId);
}
