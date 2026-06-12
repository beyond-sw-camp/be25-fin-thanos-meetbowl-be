package com.meetbowl.domain.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 공유 파일 버전 이력 도메인이 저장 기술을 모르고 영속화를 요청하기 위한 경계다. 파일별 버전 이력을 보존 조회한다. */
public interface SharedWorkspaceFileVersionRepositoryPort {

    SharedWorkspaceFileVersion save(SharedWorkspaceFileVersion version);

    Optional<SharedWorkspaceFileVersion> findById(UUID versionId);

    List<SharedWorkspaceFileVersion> findByFileId(UUID fileId);
}
