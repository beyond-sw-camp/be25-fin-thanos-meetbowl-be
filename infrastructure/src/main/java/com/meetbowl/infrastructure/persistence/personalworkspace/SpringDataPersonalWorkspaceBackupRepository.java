package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** 개인 백업 자료 엔티티의 Spring Data JPA 리포지토리다. 소유자 기준 목록과 제목 부분 일치(대소문자 무시) 검색을 제공한다. */
interface SpringDataPersonalWorkspaceBackupRepository
        extends JpaRepository<PersonalWorkspaceBackupEntity, UUID> {

    List<PersonalWorkspaceBackupEntity> findByOwnerUserIdOrderByBackedUpAtDesc(UUID ownerUserId);

    List<PersonalWorkspaceBackupEntity>
            findByOwnerUserIdAndTitleContainingIgnoreCaseOrderByBackedUpAtDesc(
                    UUID ownerUserId, String keyword);
}
