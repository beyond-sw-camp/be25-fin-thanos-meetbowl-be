package com.meetbowl.infrastructure.persistence.mail;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 메일함 항목과 메일 본문은 JPA 연관관계 없이 ID로만 연결되므로, 검색은 두 엔티티를 명시적으로 조인해 제목/본문 키워드를 함께 매칭한다.
    @Query(
            "select e from MailboxEntryEntity e, MailEntity m "
                    + "where e.mailId = m.id "
                    + "and e.ownerUserId = :ownerUserId "
                    + "and e.trashedAt is null "
                    + "and e.permanentlyDeletedAt is null "
                    + "and (lower(m.subject) like lower(concat('%', :keyword, '%')) "
                    + "or lower(m.body) like lower(concat('%', :keyword, '%')))")
    List<MailboxEntryEntity> searchByOwnerUserId(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query(
            "select count(e) from MailboxEntryEntity e, MailEntity m "
                    + "where e.mailId = m.id "
                    + "and e.ownerUserId = :ownerUserId "
                    + "and e.trashedAt is null "
                    + "and e.permanentlyDeletedAt is null "
                    + "and (lower(m.subject) like lower(concat('%', :keyword, '%')) "
                    + "or lower(m.body) like lower(concat('%', :keyword, '%')))")
    long countSearchByOwnerUserId(
            @Param("ownerUserId") UUID ownerUserId, @Param("keyword") String keyword);

    List<MailboxEntryEntity>
            findByMailboxTypeAndTrashedAtIsNullAndPermanentlyDeletedAtIsNullAndCreatedAtBefore(
                    MailboxType mailboxType, Instant cutoff, Pageable pageable);

    List<MailboxEntryEntity>
            findByTrashedAtIsNotNullAndTrashedAtBeforeAndPermanentlyDeletedAtIsNull(
                    Instant cutoff, Pageable pageable);
}
