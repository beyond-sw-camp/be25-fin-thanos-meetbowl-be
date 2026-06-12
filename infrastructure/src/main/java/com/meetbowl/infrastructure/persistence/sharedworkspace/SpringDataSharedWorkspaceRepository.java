package com.meetbowl.infrastructure.persistence.sharedworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meetbowl.domain.sharedworkspace.SharedWorkspaceVisibility;

/** 공유 워크스페이스 엔티티의 Spring Data JPA 리포지토리다. 소유자 소유분과 조직 전체 공개분을 각각 삭제 제외·최신순으로 조회한다. */
interface SpringDataSharedWorkspaceRepository extends JpaRepository<SharedWorkspaceEntity, UUID> {

    List<SharedWorkspaceEntity> findByOwnerUserIdAndDeletedAtIsNullOrderByWorkspaceCreatedAtDesc(
            UUID ownerUserId);

    List<SharedWorkspaceEntity>
            findByOrganizationIdAndVisibilityAndDeletedAtIsNullOrderByWorkspaceCreatedAtDesc(
                    UUID organizationId, SharedWorkspaceVisibility visibility);
}
