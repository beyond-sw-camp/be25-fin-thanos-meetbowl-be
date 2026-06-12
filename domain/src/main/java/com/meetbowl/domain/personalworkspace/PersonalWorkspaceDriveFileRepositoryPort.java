package com.meetbowl.domain.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 개인 드라이브 파일 메타데이터 도메인이 저장 기술을 모르고 영속화를 요청하기 위한 경계다. 목록은 삭제되지 않은 파일만 조회한다. */
public interface PersonalWorkspaceDriveFileRepositoryPort {

    PersonalWorkspaceDriveFile save(PersonalWorkspaceDriveFile file);

    Optional<PersonalWorkspaceDriveFile> findById(UUID fileId);

    List<PersonalWorkspaceDriveFile> findActiveByOwnerUserId(UUID ownerUserId);
}
