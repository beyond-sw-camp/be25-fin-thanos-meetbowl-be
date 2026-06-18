package com.meetbowl.domain.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.meetbowl.domain.common.Paged;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(UUID userId);

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByEmail(String email);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    Paged<User> findPage(String keyword, int page, int size);

    // 사용자 검색 API 전용 조회. 키워드와 조직 필터를 함께 받되, 실제 쿼리 최적화는 adapter가 담당한다.
    Paged<User> search(
            String keyword,
            UUID affiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId,
            UserStatus status,
            int page,
            int size);

    /** 같은 조직(affiliate)에 소속된 사용자 전체를 조회한다. 생성/시스템 계정 여부 판단은 호출 측 도메인 규칙에 맡긴다. */
    List<User> findAllByAffiliateId(UUID affiliateId);

    List<User> findAllByDepartmentId(UUID departmentId);

    List<User> findAllByTeamId(UUID teamId);

    List<User> findAllByPositionId(UUID positionId);
}
