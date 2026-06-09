package com.meetbowl.domain.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalWorkspaceDriveFileRepositoryPort {

    PersonalWorkspaceDriveFile save(PersonalWorkspaceDriveFile file);

    Optional<PersonalWorkspaceDriveFile> findById(UUID fileId);

    List<PersonalWorkspaceDriveFile> findActiveByOwnerUserId(UUID ownerUserId);
}
