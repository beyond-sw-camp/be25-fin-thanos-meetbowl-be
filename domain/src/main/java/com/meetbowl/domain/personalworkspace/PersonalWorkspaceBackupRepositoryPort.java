package com.meetbowl.domain.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalWorkspaceBackupRepositoryPort {

    PersonalWorkspaceBackup save(PersonalWorkspaceBackup backup);

    Optional<PersonalWorkspaceBackup> findById(UUID backupId);

    List<PersonalWorkspaceBackup> findByOwnerUserId(UUID ownerUserId);

    List<PersonalWorkspaceBackup> searchByOwnerUserIdAndKeyword(UUID ownerUserId, String keyword);
}
