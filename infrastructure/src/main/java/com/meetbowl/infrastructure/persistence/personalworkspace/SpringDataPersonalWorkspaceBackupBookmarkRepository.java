package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** 백업 자료 북마크 엔티티의 Spring Data JPA 리포지토리다. (소유자, 백업) 단위로 조회·삭제한다. */
interface SpringDataPersonalWorkspaceBackupBookmarkRepository
        extends JpaRepository<PersonalWorkspaceBackupBookmarkEntity, UUID> {

    Optional<PersonalWorkspaceBackupBookmarkEntity> findByOwnerUserIdAndBackupId(
            UUID ownerUserId, UUID backupId);

    List<PersonalWorkspaceBackupBookmarkEntity> findByOwnerUserIdOrderByBookmarkedAtDesc(
            UUID ownerUserId);

    void deleteByOwnerUserIdAndBackupId(UUID ownerUserId, UUID backupId);
}
