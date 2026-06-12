package com.meetbowl.domain.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 백업 자료 북마크 도메인이 저장 기술을 모르고 영속화를 요청하기 위한 경계다. (소유자, 백업) 단위로 조회·삭제한다. */
public interface PersonalWorkspaceBackupBookmarkRepositoryPort {

    PersonalWorkspaceBackupBookmark save(PersonalWorkspaceBackupBookmark bookmark);

    Optional<PersonalWorkspaceBackupBookmark> findByOwnerUserIdAndBackupId(
            UUID ownerUserId, UUID backupId);

    List<PersonalWorkspaceBackupBookmark> findByOwnerUserId(UUID ownerUserId);

    void deleteByOwnerUserIdAndBackupId(UUID ownerUserId, UUID backupId);
}
