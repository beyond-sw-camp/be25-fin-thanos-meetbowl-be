package com.meetbowl.domain.sharedworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SharedWorkspaceRepositoryPort {

    SharedWorkspace save(SharedWorkspace workspace);

    Optional<SharedWorkspace> findById(UUID workspaceId);

    List<SharedWorkspace> findActiveByOwnerUserId(UUID ownerUserId);

    List<SharedWorkspace> findOrganizationVisible(UUID organizationId);
}
