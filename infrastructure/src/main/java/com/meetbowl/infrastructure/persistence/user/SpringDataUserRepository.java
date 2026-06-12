package com.meetbowl.infrastructure.persistence.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.meetbowl.domain.user.UserStatus;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByLoginId(String loginId);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    @Query(
            """
            select u
            from UserEntity u
            where (:keyword is null or :keyword = ''
                or lower(u.loginId) like lower(concat('%', :keyword, '%'))
                or lower(u.name) like lower(concat('%', :keyword, '%'))
                or lower(u.email) like lower(concat('%', :keyword, '%')))
            """)
    Page<UserEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query(
            """
            select u
            from UserEntity u
            where u.role in ('USER', 'ADMIN')
              -- 조직도/수신자 검색에서는 SYSTEM 계정을 노출하지 않는다.
              and (:keyword is null or :keyword = ''
                    or lower(u.loginId) like lower(concat('%', :keyword, '%'))
                    or lower(u.name) like lower(concat('%', :keyword, '%'))
                    or lower(u.email) like lower(concat('%', :keyword, '%')))
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
}
