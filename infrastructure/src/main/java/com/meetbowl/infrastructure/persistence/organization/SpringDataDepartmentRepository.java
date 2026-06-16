package com.meetbowl.infrastructure.persistence.organization;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataDepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {

    boolean existsByAffiliateIdAndNameIgnoreCase(UUID affiliateId, String name);

    boolean existsByAffiliateIdAndNameIgnoreCaseAndIdNot(UUID affiliateId, String name, UUID id);
}
