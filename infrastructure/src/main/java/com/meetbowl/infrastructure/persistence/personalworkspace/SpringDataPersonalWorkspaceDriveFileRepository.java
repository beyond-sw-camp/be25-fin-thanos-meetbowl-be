package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** 개인 드라이브 파일 엔티티의 Spring Data JPA 리포지토리다. 목록은 삭제되지 않은 파일만 최신 업로드순으로 조회한다. */
interface SpringDataPersonalWorkspaceDriveFileRepository
        extends JpaRepository<PersonalWorkspaceDriveFileEntity, UUID> {

    List<PersonalWorkspaceDriveFileEntity> findByOwnerUserIdAndDeletedAtIsNullOrderByUploadedAtDesc(
            UUID ownerUserId);
}
