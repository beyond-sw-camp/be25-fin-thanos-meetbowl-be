package com.meetbowl.infrastructure.persistence.organization;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataTeamRepository extends JpaRepository<TeamEntity, UUID> {

    @Query(
            """
            select team
            from TeamEntity team
            left join DepartmentEntity department on department.id = team.departmentId
            left join AffiliateEntity affiliate on affiliate.id = department.affiliateId
            order by
                case when affiliate.sortOrder is null then 1 else 0 end,
                affiliate.sortOrder asc,
                lower(coalesce(affiliate.name, '')) asc,
                case when department.sortOrder is null then 1 else 0 end,
                department.sortOrder asc,
                lower(coalesce(department.name, '')) asc,
                case when team.sortOrder is null then 1 else 0 end,
                team.sortOrder asc,
                lower(team.name) asc,
                team.id asc
            """)
    java.util.List<TeamEntity> findAllForExcelExport();

    boolean existsByDepartmentIdAndNameIgnoreCase(UUID departmentId, String name);

    boolean existsByDepartmentIdAndNameIgnoreCaseAndIdNot(UUID departmentId, String name, UUID id);
}
