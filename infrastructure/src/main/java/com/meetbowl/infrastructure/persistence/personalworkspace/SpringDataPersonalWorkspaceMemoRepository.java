package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPersonalWorkspaceMemoRepository
        extends JpaRepository<PersonalWorkspaceMemoEntity, UUID> {

    List<PersonalWorkspaceMemoEntity> findByOwnerUserIdOrderByMemoUpdatedAtDesc(UUID ownerUserId);
}
