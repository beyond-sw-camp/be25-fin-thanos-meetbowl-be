package com.meetbowl.infrastructure.persistence.organization;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataDepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {

    @Query(
            """
            select department
            from DepartmentEntity department
            left join AffiliateEntity affiliate on affiliate.id = department.affiliateId
            order by
                case when affiliate.sortOrder is null then 1 else 0 end,
                affiliate.sortOrder asc,
                lower(coalesce(affiliate.name, '')) asc,
                case when department.sortOrder is null then 1 else 0 end,
                department.sortOrder asc,
                lower(department.name) asc,
                department.id asc
            """)
    java.util.List<DepartmentEntity> findAllForExcelExport();

    boolean existsByAffiliateIdAndNameIgnoreCase(UUID affiliateId, String name);

    boolean existsByAffiliateIdAndNameIgnoreCaseAndIdNot(UUID affiliateId, String name, UUID id);
}
