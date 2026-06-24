package com.meetbowl.infrastructure.persistence.organization;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    java.util.List<TeamEntity> findByDepartmentId(UUID departmentId);

    boolean existsByDepartmentIdAndNameIgnoreCase(UUID departmentId, String name);

    boolean existsByDepartmentIdAndNameIgnoreCaseAndIdNot(UUID departmentId, String name, UUID id);

    @Query(
            """
            select (count(team) > 0)
            from TeamEntity team
            join DepartmentEntity department on department.id = team.departmentId
            where department.affiliateId = :affiliateId
              and team.sortOrder = :sortOrder
            """)
    boolean existsByAffiliateIdAndSortOrder(
            @Param("affiliateId") UUID affiliateId, @Param("sortOrder") Integer sortOrder);

    @Query(
            """
            select (count(team) > 0)
            from TeamEntity team
            join DepartmentEntity department on department.id = team.departmentId
            where department.affiliateId = :affiliateId
              and team.sortOrder = :sortOrder
              and team.id <> :id
            """)
    boolean existsByAffiliateIdAndSortOrderAndIdNot(
            @Param("affiliateId") UUID affiliateId,
            @Param("sortOrder") Integer sortOrder,
            @Param("id") UUID id);
}
