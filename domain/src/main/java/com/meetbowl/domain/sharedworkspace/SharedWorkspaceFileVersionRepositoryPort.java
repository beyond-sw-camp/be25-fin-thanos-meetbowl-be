package com.meetbowl.domain.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SharedWorkspaceFileVersionRepositoryPort {

    SharedWorkspaceFileVersion save(SharedWorkspaceFileVersion version);

    Optional<SharedWorkspaceFileVersion> findById(UUID versionId);

    List<SharedWorkspaceFileVersion> findByFileId(UUID fileId);
}
