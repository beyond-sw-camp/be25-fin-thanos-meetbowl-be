package com.meetbowl.infrastructure.persistence.user;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

    @Query(
            """
            select u
            from UserEntity u
            where u.role in :roles
              and u.deletedAt is null
            order by lower(u.name) asc, lower(u.email) asc, lower(u.loginId) asc, u.id asc
            """)
    List<UserEntity> findAllForExcelExportByRoles(@Param("roles") Set<UserRole> roles);

    Optional<UserEntity> findByLoginIdAndDeletedAtIsNull(String loginId);

    Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByLoginIdAndDeletedAtIsNull(String loginId);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    Optional<UserEntity> findByIdAndDeletedAtIsNull(UUID userId);

    // ?ъ슜???대쫫/?대찓??濡쒓렇??ID肉??꾨땲??議곗쭅紐낃낵 沅뚰븳 ?쒖떆紐낃퉴吏 遺遺꾧??됲븳??
    @Query(
            """
            select u
            from UserEntity u
            left join AffiliateEntity affiliate on affiliate.id = u.affiliateId
            left join DepartmentEntity department on department.id = u.departmentId
            left join TeamEntity team on team.id = u.teamId
            left join PositionEntity position on position.id = u.positionId
            where u.role in ('USER', 'ADMIN')
              and u.deletedAt is null
              and (:keyword is null or :keyword = ''
                or lower(u.loginId) like lower(concat('%', :keyword, '%'))
                or lower(u.name) like lower(concat('%', :keyword, '%'))
                or lower(u.email) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(affiliate.name, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(department.name, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(team.name, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(position.name, '')) like lower(concat('%', :keyword, '%'))
                or lower(
                        case
                            when u.role = com.meetbowl.domain.user.UserRole.ADMIN then 'ADMIN'
                            when u.role = com.meetbowl.domain.user.UserRole.USER then 'USER'
                            else ''
                        end)
                    like lower(concat('%', :keyword, '%'))
                or lower(
                        case
                            when u.role = com.meetbowl.domain.user.UserRole.ADMIN then '愿由ъ옄'
                            when u.role = com.meetbowl.domain.user.UserRole.USER then '?쇰컲 ?ъ슜??'
                            else ''
                        end)
                    like lower(concat('%', :keyword, '%')))
            """)
    Page<UserEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ?쇰컲 ?ъ슜??寃?됱? 愿由ъ옄 ?꾩슜 ?뺣낫瑜??몄텧?섏? ?딆쑝硫댁꽌 議곗쭅紐낃퉴吏 媛숈씠 寃?됲븳??
    @Query(
            """
            select u
            from UserEntity u
            left join AffiliateEntity affiliate on affiliate.id = u.affiliateId
            left join DepartmentEntity department on department.id = u.departmentId
            left join TeamEntity team on team.id = u.teamId
            left join PositionEntity position on position.id = u.positionId
            where u.role in ('USER', 'ADMIN')
              and u.deletedAt is null
              and (:keyword is null or :keyword = ''
                    or lower(u.loginId) like lower(concat('%', :keyword, '%'))
                    or lower(u.name) like lower(concat('%', :keyword, '%'))
                    or lower(u.email) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(affiliate.name, '')) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(department.name, '')) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(team.name, '')) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(position.name, '')) like lower(concat('%', :keyword, '%')))
              and (:affiliateId is null or u.affiliateId = :affiliateId)
              and (:departmentId is null or u.departmentId = :departmentId)
              and (:teamId is null or u.teamId = :teamId)
              and (:positionId is null or u.positionId = :positionId)
              and (
                    :status is null
                    or (:status = com.meetbowl.domain.user.UserStatus.ACTIVE
                        and u.status = com.meetbowl.domain.user.UserStatus.ACTIVE
                        and (u.activeFrom is null or u.activeFrom < :nextDayStart)
                        and (u.activeUntil is null or u.activeUntil >= :dayStart))
                    or (:status = com.meetbowl.domain.user.UserStatus.INACTIVE
                        and (
                            u.status = com.meetbowl.domain.user.UserStatus.INACTIVE
                            or (u.status = com.meetbowl.domain.user.UserStatus.ACTIVE
                                and (
                                    (u.activeFrom is not null and u.activeFrom >= :nextDayStart)
                                    or (u.activeUntil is not null and u.activeUntil < :dayStart)
                                ))
                        ))
                    or (:status = com.meetbowl.domain.user.UserStatus.LOCKED
                        and u.status = com.meetbowl.domain.user.UserStatus.LOCKED)
                  )
            """)
    Page<UserEntity> searchUsers(
            @Param("keyword") String keyword,
            @Param("affiliateId") UUID affiliateId,
            @Param("departmentId") UUID departmentId,
            @Param("teamId") UUID teamId,
            @Param("positionId") UUID positionId,
            @Param("status") UserStatus status,
            @Param("dayStart") Instant dayStart,
            @Param("nextDayStart") Instant nextDayStart,
            Pageable pageable);

    List<UserEntity> findByAffiliateIdAndDeletedAtIsNull(UUID affiliateId);

    List<UserEntity> findByDepartmentIdAndDeletedAtIsNull(UUID departmentId);

    List<UserEntity> findByTeamIdAndDeletedAtIsNull(UUID teamId);

    List<UserEntity> findByPositionIdAndDeletedAtIsNull(UUID positionId);

    @Query(
            """
            select new com.meetbowl.infrastructure.persistence.user.UserSearchSourceRow(
                u.id,
                u.loginId,
                u.name,
                u.email,
                u.role,
                u.status,
                u.affiliateId,
                affiliate.name,
                u.departmentId,
                department.name,
                u.teamId,
                team.name,
                u.positionId,
                position.name,
                u.activeFrom,
                u.activeUntil,
                u.deletedAt,
                u.createdAt)
            from UserEntity u
            left join AffiliateEntity affiliate on affiliate.id = u.affiliateId
            left join DepartmentEntity department on department.id = u.departmentId
            left join TeamEntity team on team.id = u.teamId
            left join PositionEntity position on position.id = u.positionId
            where u.role in ('USER', 'ADMIN')
              and u.id in :userIds
            """)
    List<UserSearchSourceRow> findSearchSourcesByIdIn(@Param("userIds") Collection<UUID> userIds);

    @Query(
            """
            select new com.meetbowl.infrastructure.persistence.user.UserSearchSourceRow(
                u.id,
                u.loginId,
                u.name,
                u.email,
                u.role,
                u.status,
                u.affiliateId,
                affiliate.name,
                u.departmentId,
                department.name,
                u.teamId,
                team.name,
                u.positionId,
                position.name,
                u.activeFrom,
                u.activeUntil,
                u.deletedAt,
                u.createdAt)
            from UserEntity u
            left join AffiliateEntity affiliate on affiliate.id = u.affiliateId
            left join DepartmentEntity department on department.id = u.departmentId
            left join TeamEntity team on team.id = u.teamId
            left join PositionEntity position on position.id = u.positionId
            where u.role in ('USER', 'ADMIN')
            """)
    Page<UserSearchSourceRow> findAllSearchSources(Pageable pageable);

    @Query(
            """
            select new com.meetbowl.infrastructure.persistence.user.UserSearchSourceRow(
                u.id,
                u.loginId,
                u.name,
                u.email,
                u.role,
                u.status,
                u.affiliateId,
                affiliate.name,
                u.departmentId,
                department.name,
                u.teamId,
                team.name,
                u.positionId,
                position.name,
                u.activeFrom,
                u.activeUntil,
                u.deletedAt,
                u.createdAt)
            from UserEntity u
            left join AffiliateEntity affiliate on affiliate.id = u.affiliateId
            left join DepartmentEntity department on department.id = u.departmentId
            left join TeamEntity team on team.id = u.teamId
            left join PositionEntity position on position.id = u.positionId
            where u.role in ('USER', 'ADMIN')
              and u.affiliateId = :affiliateId
            """)
    List<UserSearchSourceRow> findSearchSourcesByAffiliateId(
            @Param("affiliateId") UUID affiliateId);

    @Query(
            """
            select new com.meetbowl.infrastructure.persistence.user.UserSearchSourceRow(
                u.id,
                u.loginId,
                u.name,
                u.email,
                u.role,
                u.status,
                u.affiliateId,
                affiliate.name,
                u.departmentId,
                department.name,
                u.teamId,
                team.name,
                u.positionId,
                position.name,
                u.activeFrom,
                u.activeUntil,
                u.deletedAt,
                u.createdAt)
            from UserEntity u
            left join AffiliateEntity affiliate on affiliate.id = u.affiliateId
            left join DepartmentEntity department on department.id = u.departmentId
            left join TeamEntity team on team.id = u.teamId
            left join PositionEntity position on position.id = u.positionId
            where u.role in ('USER', 'ADMIN')
              and u.departmentId = :departmentId
            """)
    List<UserSearchSourceRow> findSearchSourcesByDepartmentId(
            @Param("departmentId") UUID departmentId);

    @Query(
            """
            select new com.meetbowl.infrastructure.persistence.user.UserSearchSourceRow(
                u.id,
                u.loginId,
                u.name,
                u.email,
                u.role,
                u.status,
                u.affiliateId,
                affiliate.name,
                u.departmentId,
                department.name,
                u.teamId,
                team.name,
                u.positionId,
                position.name,
                u.activeFrom,
                u.activeUntil,
                u.deletedAt,
                u.createdAt)
            from UserEntity u
            left join AffiliateEntity affiliate on affiliate.id = u.affiliateId
            left join DepartmentEntity department on department.id = u.departmentId
            left join TeamEntity team on team.id = u.teamId
            left join PositionEntity position on position.id = u.positionId
            where u.role in ('USER', 'ADMIN')
              and u.teamId = :teamId
            """)
    List<UserSearchSourceRow> findSearchSourcesByTeamId(@Param("teamId") UUID teamId);

    @Query(
            """
            select new com.meetbowl.infrastructure.persistence.user.UserSearchSourceRow(
                u.id,
                u.loginId,
                u.name,
                u.email,
                u.role,
                u.status,
                u.affiliateId,
                affiliate.name,
                u.departmentId,
                department.name,
                u.teamId,
                team.name,
                u.positionId,
                position.name,
                u.activeFrom,
                u.activeUntil,
                u.deletedAt,
                u.createdAt)
            from UserEntity u
            left join AffiliateEntity affiliate on affiliate.id = u.affiliateId
            left join DepartmentEntity department on department.id = u.departmentId
            left join TeamEntity team on team.id = u.teamId
            left join PositionEntity position on position.id = u.positionId
            where u.role in ('USER', 'ADMIN')
              and u.positionId = :positionId
            """)
    List<UserSearchSourceRow> findSearchSourcesByPositionId(@Param("positionId") UUID positionId);
}
