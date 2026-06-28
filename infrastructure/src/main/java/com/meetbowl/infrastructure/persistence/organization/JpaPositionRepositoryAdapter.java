package com.meetbowl.infrastructure.persistence.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.organization.Position;
import com.meetbowl.domain.organization.PositionRepositoryPort;

@Repository
public class JpaPositionRepositoryAdapter implements PositionRepositoryPort {
    private final SpringDataPositionRepository repository;

    public JpaPositionRepositoryAdapter(SpringDataPositionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Position save(Position position) {
        return repository.save(PositionEntity.from(position)).toDomain();
    }

    @Override
    public void deleteById(UUID positionId) {
        repository.deleteById(positionId);
    }

    @Override
    public Optional<Position> findById(UUID positionId) {
        return repository.findById(positionId).map(PositionEntity::toDomain);
    }

    @Override
    public List<Position> findAll() {
        return repository.findAll().stream().map(PositionEntity::toDomain).toList();
    }

    @Override
    public List<Position> findAllForExcelExport() {
        return repository.findAllForExcelExport().stream().map(PositionEntity::toDomain).toList();
    }

    @Override
    public List<Position> findAllByAffiliateId(UUID affiliateId) {
        return repository.findAllByAffiliateId(affiliateId).stream()
                .map(PositionEntity::toDomain)
                .toList();
    }

    @Override
    public List<Position> findAllByIds(Collection<UUID> positionIds) {
        return repository.findAllById(positionIds).stream().map(PositionEntity::toDomain).toList();
    }

    @Override
    public boolean existsByAffiliateIdAndName(UUID affiliateId, String name) {
        return repository.existsByAffiliateIdAndNameIgnoreCase(affiliateId, name);
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCodeIgnoreCase(code);
    }

    @Override
    public boolean existsByAffiliateIdAndNameAndIdNot(
            UUID affiliateId, String name, UUID positionId) {
        return repository.existsByAffiliateIdAndNameIgnoreCaseAndIdNot(
                affiliateId, name, positionId);
    }

    @Override
    public boolean existsByCodeAndIdNot(String code, UUID positionId) {
        return repository.existsByCodeIgnoreCaseAndIdNot(code, positionId);
    }

    @Override
    public boolean existsByAffiliateIdAndSortOrder(UUID affiliateId, Integer sortOrder) {
        return repository.existsByAffiliateIdAndSortOrder(affiliateId, sortOrder);
    }

    @Override
    public boolean existsByAffiliateIdAndSortOrderAndIdNot(
            UUID affiliateId, Integer sortOrder, UUID positionId) {
        return repository.existsByAffiliateIdAndSortOrderAndIdNot(
                affiliateId, sortOrder, positionId);
    }
}
