package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** 공유 파일 엔티티의 Spring Data JPA 리포지토리다. 새 버전 동시 추가 충돌을 막기 위해 비관적 쓰기 잠금 조회를 제공한다. */
interface SpringDataSharedWorkspaceFileRepository
        extends JpaRepository<SharedWorkspaceFileEntity, UUID> {

    List<SharedWorkspaceFileEntity> findByWorkspaceIdAndDeletedAtIsNullOrderByUploadedAtDesc(
            UUID workspaceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<SharedWorkspaceFileEntity> findForUpdateById(UUID fileId);
}
