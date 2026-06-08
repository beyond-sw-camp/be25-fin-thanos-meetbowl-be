package com.meetbowl.domain.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SharedWorkspaceFileRepositoryPort {

    SharedWorkspaceFile save(SharedWorkspaceFile file);

    Optional<SharedWorkspaceFile> findById(UUID fileId);

    List<SharedWorkspaceFile> findActiveByWorkspaceId(UUID workspaceId);
}
