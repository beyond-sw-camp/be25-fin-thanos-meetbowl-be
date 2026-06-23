package com.meetbowl.infrastructure.persistence.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.organization.Department;
import com.meetbowl.domain.organization.DepartmentRepositoryPort;

@Repository
public class JpaDepartmentRepositoryAdapter implements DepartmentRepositoryPort {
    private final SpringDataDepartmentRepository repository;

    public JpaDepartmentRepositoryAdapter(SpringDataDepartmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Department save(Department department) {
        return repository.save(DepartmentEntity.from(department)).toDomain();
    }

    @Override
    public void deleteById(UUID departmentId) {
        repository.deleteById(departmentId);
    }

    @Override
    public Optional<Department> findById(UUID departmentId) {
        return repository.findById(departmentId).map(DepartmentEntity::toDomain);
    }

    @Override
    public List<Department> findAll() {
        return repository.findAll().stream().map(DepartmentEntity::toDomain).toList();
    }

    @Override
    public List<Department> findAllForExcelExport() {
        return repository.findAllForExcelExport().stream().map(DepartmentEntity::toDomain).toList();
    }

    @Override
    public List<Department> findAllByIds(Collection<UUID> departmentIds) {
        return repository.findAllById(departmentIds).stream()
                .map(DepartmentEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByAffiliateIdAndName(UUID affiliateId, String name) {
        return repository.existsByAffiliateIdAndNameIgnoreCase(affiliateId, name);
    }

    @Override
    public boolean existsByAffiliateIdAndNameAndIdNot(
            UUID affiliateId, String name, UUID departmentId) {
        return repository.existsByAffiliateIdAndNameIgnoreCaseAndIdNot(
                affiliateId, name, departmentId);
    }
}
