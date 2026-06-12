package com.meetbowl.domain.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(UUID userId);

    Optional<User> findByLoginId(String loginId);

    /** 같은 조직(affiliate)에 소속된 사용자 전체를 조회한다. 활성/시스템 계정 여부 판단은 호출 측 도메인 규칙에 맡긴다. */
    List<User> findAllByAffiliateId(UUID affiliateId);
}
