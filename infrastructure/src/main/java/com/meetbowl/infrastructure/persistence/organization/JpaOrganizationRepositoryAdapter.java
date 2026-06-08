package com.meetbowl.infrastructure.persistence.organization;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.organization.Organization;
import com.meetbowl.domain.organization.OrganizationRepositoryPort;

@Repository
public class JpaOrganizationRepositoryAdapter implements OrganizationRepositoryPort {

    private final SpringDataOrganizationRepository springDataOrganizationRepository;

    public JpaOrganizationRepositoryAdapter(
            SpringDataOrganizationRepository springDataOrganizationRepository) {
        this.springDataOrganizationRepository = springDataOrganizationRepository;
    }

    @Override
    public Organization save(Organization organization) {
        return springDataOrganizationRepository
                .save(OrganizationEntity.from(organization))
                .toDomain();
    }

    @Override
    public Optional<Organization> findById(UUID organizationId) {
        return springDataOrganizationRepository
                .findById(organizationId)
                .map(OrganizationEntity::toDomain);
    }
}
