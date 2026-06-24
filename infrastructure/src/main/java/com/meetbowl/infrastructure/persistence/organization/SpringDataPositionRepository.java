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

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    boolean existsBySortOrder(Integer sortOrder);

    boolean existsBySortOrderAndIdNot(Integer sortOrder, UUID id);
}
