package com.meetbowl.domain.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 개인 백업 자료 도메인이 저장 기술을 모르고 영속화를 요청하기 위한 경계다. 소유자 기준 목록 조회와 제목 키워드 검색을 제공한다. */
public interface PersonalWorkspaceBackupRepositoryPort {

    PersonalWorkspaceBackup save(PersonalWorkspaceBackup backup);

    Optional<PersonalWorkspaceBackup> findById(UUID backupId);

    List<PersonalWorkspaceBackup> findByOwnerUserId(UUID ownerUserId);

    List<PersonalWorkspaceBackup> searchByOwnerUserIdAndKeyword(UUID ownerUserId, String keyword);
}
