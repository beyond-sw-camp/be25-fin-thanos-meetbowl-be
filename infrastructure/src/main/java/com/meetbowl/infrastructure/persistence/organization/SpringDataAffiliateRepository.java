package com.meetbowl.infrastructure.persistence.organization;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataAffiliateRepository extends JpaRepository<AffiliateEntity, UUID> {

    @Query(
            """
            select affiliate
            from AffiliateEntity affiliate
            order by
                case when affiliate.sortOrder is null then 1 else 0 end,
                affiliate.sortOrder asc,
                lower(affiliate.name) asc,
                affiliate.id asc
            """)
    java.util.List<AffiliateEntity> findAllForExcelExport();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}
