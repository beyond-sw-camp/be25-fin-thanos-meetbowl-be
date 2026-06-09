package com.meetbowl.domain.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalWorkspaceBackupBookmarkRepositoryPort {

    PersonalWorkspaceBackupBookmark save(PersonalWorkspaceBackupBookmark bookmark);

    Optional<PersonalWorkspaceBackupBookmark> findByOwnerUserIdAndBackupId(
            UUID ownerUserId, UUID backupId);

    List<PersonalWorkspaceBackupBookmark> findByOwnerUserId(UUID ownerUserId);

    void deleteByOwnerUserIdAndBackupId(UUID ownerUserId, UUID backupId);
}
