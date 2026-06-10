package com.meetbowl.domain.community;

import java.util.Optional;
import java.util.UUID;

/** 커뮤니티 익명 표시명 매핑 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface CommunityAliasRepositoryPort {

    CommunityAlias save(CommunityAlias alias);

    Optional<CommunityAlias> findByUserId(UUID userId);

    /** 다음 익명 번호 산정용 현재 매핑 수. (동시성은 상위 계층 또는 유니크 제약+재시도로 보장) */
    long count();
}