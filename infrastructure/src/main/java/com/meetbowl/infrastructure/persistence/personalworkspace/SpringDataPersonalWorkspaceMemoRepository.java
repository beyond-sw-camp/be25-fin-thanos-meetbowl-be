package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/** 개인 메모 엔티티의 Spring Data JPA 리포지토리다. 조회·삭제를 소유자 ID와 함께 수행해 소유권 경계를 강제한다. */
interface SpringDataPersonalWorkspaceMemoRepository
        extends JpaRepository<PersonalWorkspaceMemoEntity, UUID> {

    Optional<PersonalWorkspaceMemoEntity> findByIdAndOwnerUserId(UUID memoId, UUID ownerUserId);

    List<PersonalWorkspaceMemoEntity> findByOwnerUserIdOrderByMemoUpdatedAtDesc(UUID ownerUserId);

    @Transactional
    long deleteByIdAndOwnerUserId(UUID memoId, UUID ownerUserId);
}
