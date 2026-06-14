package com.meetbowl.application.community;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.community.CommunityAlias;
import com.meetbowl.domain.community.CommunityAliasRepositoryPort;

/**
 * 사용자별 익명 번호(aliasNo)를 한 번 발급/재사용하는 채번기다.
 *
 * <p>각 발급 시도를 {@link Propagation#REQUIRES_NEW 독립 트랜잭션}으로 격리한다. 이렇게 해야 동시 첫 활동으로 유니크 제약(user_id /
 * alias_no)에 걸려 실패해도 그 실패가 호출자(게시글 저장) 트랜잭션을 오염시키지 않고, 재시도 시 직전 경쟁자가 커밋한 행을 새 트랜잭션에서 다시 읽을 수 있다. 충돌
 * 시 재시도는 {@link CommunityAliasResolver}가 담당한다.
 *
 * <p>채번 방식: 별칭은 영구·불변(삭제하지 않음)이므로 현재 매핑 수 + 1 이 다음 번호다. 최종 충돌 방어선은 DB의 alias_no/user_id 유니크 제약이다.
 */
@Component
public class CommunityAliasAllocator {

    private final CommunityAliasRepositoryPort communityAliasRepositoryPort;

    public CommunityAliasAllocator(CommunityAliasRepositoryPort communityAliasRepositoryPort) {
        this.communityAliasRepositoryPort = communityAliasRepositoryPort;
    }

    /**
     * 이미 별칭이 있으면 그대로 재사용하고, 없으면 다음 번호로 새로 발급한다. 동시 삽입으로 유니크 제약에 걸리면 이 트랜잭션이 롤백되며 예외가 전파된다(재시도는 상위에서
     * 처리).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CommunityAlias allocate(UUID userId) {
        return communityAliasRepositoryPort
                .findByUserId(userId)
                .orElseGet(
                        () ->
                                communityAliasRepositoryPort.save(
                                        CommunityAlias.create(userId, nextAliasNo())));
    }

    private int nextAliasNo() {
        return (int) (communityAliasRepositoryPort.count() + 1);
    }
}
