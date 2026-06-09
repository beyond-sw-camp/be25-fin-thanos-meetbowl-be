package com.meetbowl.infrastructure.persistence.organization;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.organization.Affiliate;
import com.meetbowl.domain.organization.AffiliateRepositoryPort;

@Repository
public class JpaAffiliateRepositoryAdapter implements AffiliateRepositoryPort {

    private final SpringDataAffiliateRepository springDataOrganizationRepository;

    public JpaAffiliateRepositoryAdapter(
            SpringDataAffiliateRepository springDataOrganizationRepository) {
        this.springDataOrganizationRepository = springDataOrganizationRepository;
    }

    @Override
    public Affiliate save(Affiliate organization) {
        return springDataOrganizationRepository
                .save(AffiliateEntity.from(organization))
                .toDomain();
    }

    @Override
    public Optional<Affiliate> findById(UUID organizationId) {
        return springDataOrganizationRepository
                .findById(organizationId)
                .map(AffiliateEntity::toDomain);
    }
}
