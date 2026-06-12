package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** 공유 파일 버전 이력 엔티티의 Spring Data JPA 리포지토리다. 버전 이력을 major.minor.patch 내림차순(최신 먼저)으로 조회한다. */
interface SpringDataSharedWorkspaceFileVersionRepository
        extends JpaRepository<SharedWorkspaceFileVersionEntity, UUID> {

    List<SharedWorkspaceFileVersionEntity>
            findByFileIdOrderByVersionMajorDescVersionMinorDescVersionPatchDesc(UUID fileId);
}
