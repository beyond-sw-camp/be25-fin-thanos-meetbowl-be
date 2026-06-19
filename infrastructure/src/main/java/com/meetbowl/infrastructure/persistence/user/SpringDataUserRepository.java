package com.meetbowl.infrastructure.persistence.user;

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
            order by lower(u.name) asc, lower(u.email) asc, lower(u.loginId) asc, u.id asc
            """)
    List<UserEntity> findAllForExcelExportByRoles(@Param("roles") Set<UserRole> roles);

    Optional<UserEntity> findByLoginId(String loginId);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    // 사용자 이름/이메일/로그인 ID뿐 아니라 조직명과 권한 표시명까지 부분검색한다.
    @Query(
            """
            select u
            from UserEntity u
            left join AffiliateEntity affiliate on affiliate.id = u.affiliateId
            left join DepartmentEntity department on department.id = u.departmentId
            left join TeamEntity team on team.id = u.teamId
            left join PositionEntity position on position.id = u.positionId
            where u.role in ('USER', 'ADMIN')
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
                            when u.role = com.meetbowl.domain.user.UserRole.ADMIN then '관리자'
                            when u.role = com.meetbowl.domain.user.UserRole.USER then '일반 사용자'
                            else ''
                        end)
                    like lower(concat('%', :keyword, '%')))
            """)
    Page<UserEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 일반 사용자 검색은 관리자 전용 정보를 노출하지 않으면서 조직명까지 같이 검색한다.
    @Query(
            """
            select u
            from UserEntity u
            left join AffiliateEntity affiliate on affiliate.id = u.affiliateId
            left join DepartmentEntity department on department.id = u.departmentId
            left join TeamEntity team on team.id = u.teamId
            left join PositionEntity position on position.id = u.positionId
            where u.role in ('USER', 'ADMIN')
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
              and (:status is null or u.status = :status)
            """)
    Page<UserEntity> searchUsers(
            @Param("keyword") String keyword,
            @Param("affiliateId") UUID affiliateId,
            @Param("departmentId") UUID departmentId,
            @Param("teamId") UUID teamId,
            @Param("positionId") UUID positionId,
            @Param("status") UserStatus status,
            Pageable pageable);

    List<UserEntity> findByAffiliateId(UUID affiliateId);

    List<UserEntity> findByDepartmentId(UUID departmentId);

    List<UserEntity> findByTeamId(UUID teamId);

    List<UserEntity> findByPositionId(UUID positionId);

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
