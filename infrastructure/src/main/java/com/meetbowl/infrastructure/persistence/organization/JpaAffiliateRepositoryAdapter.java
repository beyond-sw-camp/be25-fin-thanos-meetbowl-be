package com.meetbowl.infrastructure.persistence.organization;

import java.util.Collection;
import java.util.List;
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
        return springDataOrganizationRepository.save(AffiliateEntity.from(organization)).toDomain();
    }

    @Override
    public Optional<Affiliate> findById(UUID organizationId) {
        return springDataOrganizationRepository
                .findById(organizationId)
                .map(AffiliateEntity::toDomain);
    }

    @Override
    public List<Affiliate> findAll() {
        return springDataOrganizationRepository.findAll().stream()
                .map(AffiliateEntity::toDomain)
                .toList();
    }

    @Override
    public List<Affiliate> findAllForExcelExport() {
        return springDataOrganizationRepository.findAllForExcelExport().stream()
                .map(AffiliateEntity::toDomain)
                .toList();
    }

    @Override
    public List<Affiliate> findAllByIds(Collection<UUID> organizationIds) {
        return springDataOrganizationRepository.findAllById(organizationIds).stream()
                .map(AffiliateEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByName(String name) {
        return springDataOrganizationRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByCode(String code) {
        return springDataOrganizationRepository.existsByCodeIgnoreCase(code);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, UUID affiliateId) {
        return springDataOrganizationRepository.existsByNameIgnoreCaseAndIdNot(name, affiliateId);
    }

    @Override
    public boolean existsByCodeAndIdNot(String code, UUID affiliateId) {
        return springDataOrganizationRepository.existsByCodeIgnoreCaseAndIdNot(code, affiliateId);
    }
}
