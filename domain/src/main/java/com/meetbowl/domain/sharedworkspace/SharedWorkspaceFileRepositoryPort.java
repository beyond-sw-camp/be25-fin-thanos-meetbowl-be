package com.meetbowl.domain.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 공유 파일 도메인이 저장 기술을 모르고 영속화를 요청하기 위한 경계다. 새 버전 동시 추가를 위한 잠금 조회를 별도로 제공한다. */
public interface SharedWorkspaceFileRepositoryPort {

    SharedWorkspaceFile save(SharedWorkspaceFile file);

    Optional<SharedWorkspaceFile> findById(UUID fileId);

    Optional<SharedWorkspaceFile> findByIdForUpdate(UUID fileId);

    List<SharedWorkspaceFile> findActiveByWorkspaceId(UUID workspaceId);
}
