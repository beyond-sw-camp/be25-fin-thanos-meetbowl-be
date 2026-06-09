package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface SpringDataPersonalWorkspaceMemoRepository
        extends JpaRepository<PersonalWorkspaceMemoEntity, UUID> {

    Optional<PersonalWorkspaceMemoEntity> findByIdAndOwnerUserId(UUID memoId, UUID ownerUserId);

    List<PersonalWorkspaceMemoEntity> findByOwnerUserIdOrderByMemoUpdatedAtDesc(UUID ownerUserId);

    @Transactional
    long deleteByIdAndOwnerUserId(UUID memoId, UUID ownerUserId);
}
