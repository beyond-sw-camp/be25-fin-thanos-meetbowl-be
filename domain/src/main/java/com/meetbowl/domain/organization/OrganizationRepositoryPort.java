package com.meetbowl.domain.organization;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepositoryPort {

    Organization save(Organization organization);

    Optional<Organization> findById(UUID organizationId);
}
