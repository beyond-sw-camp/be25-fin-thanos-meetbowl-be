package com.meetbowl.domain.community;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 커뮤니티 익명 표시명 매핑 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface CommunityAliasRepositoryPort {

    CommunityAlias save(CommunityAlias alias);

    Optional<CommunityAlias> findByUserId(UUID userId);

    /** 여러 사용자의 별칭을 한 번에 조회한다. 목록 화면에서 작성자별 "익명N" 표시명을 N+1 없이 매핑하기 위한 배치 조회다. */
    List<CommunityAlias> findByUserIdIn(Collection<UUID> userIds);

    /** 다음 익명 번호 산정용 현재 매핑 수. (동시성은 상위 계층 또는 유니크 제약+재시도로 보장) */
    long count();
}
