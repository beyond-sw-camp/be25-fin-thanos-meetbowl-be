package com.meetbowl.infrastructure.persistence.organization;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataPositionRepository extends JpaRepository<PositionEntity, UUID> {

    @Query(
            """
            select position
            from PositionEntity position
            order by
                case when position.sortOrder is null then 1 else 0 end,
                position.sortOrder asc,
                lower(position.name) asc,
                position.id asc
            """)
    java.util.List<PositionEntity> findAllForExcelExport();

    java.util.List<PositionEntity> findAllByAffiliateId(java.util.UUID affiliateId);

    boolean existsByAffiliateIdAndNameIgnoreCase(java.util.UUID affiliateId, String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByAffiliateIdAndNameIgnoreCaseAndIdNot(
            java.util.UUID affiliateId, String name, UUID id);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    boolean existsByAffiliateIdAndSortOrder(java.util.UUID affiliateId, Integer sortOrder);

    boolean existsByAffiliateIdAndSortOrderAndIdNot(
            java.util.UUID affiliateId, Integer sortOrder, UUID id);
}
