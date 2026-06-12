package com.meetbowl.domain.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 공유 워크스페이스 도메인이 저장 기술을 모르고 영속화를 요청하기 위한 경계다. 소유자 소유분과 조직 전체 공개분을 따로 조회한다. */
public interface SharedWorkspaceRepositoryPort {

    SharedWorkspace save(SharedWorkspace workspace);

    Optional<SharedWorkspace> findById(UUID workspaceId);

    List<SharedWorkspace> findActiveByOwnerUserId(UUID ownerUserId);

    List<SharedWorkspace> findOrganizationVisible(UUID organizationId);
}
