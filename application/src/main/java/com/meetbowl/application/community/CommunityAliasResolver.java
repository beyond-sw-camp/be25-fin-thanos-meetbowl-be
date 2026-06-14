package com.meetbowl.application.community;

import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.CommunityAlias;

/**
 * 게시판 첫 활동(글/댓글) 시 작성자의 익명 별칭을 안전하게 확보한다.
 *
 * <p>실제 발급은 {@link CommunityAliasAllocator}(독립 트랜잭션)에 위임하고, 여기서는 동시 첫 활동으로 인한 유니크 제약 충돌을 짧게 재시도한다.
 * 충돌 유형은 두 가지이며 재시도로 모두 수렴한다.
 *
 * <ul>
 *   <li>같은 사용자의 동시 요청: 다음 시도의 findByUserId가 경쟁자가 커밋한 별칭을 읽어 그대로 재사용한다(멱등).
 *   <li>서로 다른 사용자의 번호 충돌: 다음 시도에서 count 기반 번호를 다시 계산해 빈 번호로 발급된다.
 * </ul>
 *
 * <p>발급/재사용 실패가 반복되면(드문 고경합) {@link ErrorCode#COMMON_CONFLICT}로 실패시켜 호출자가 재요청하게 한다.
 */
@Component
public class CommunityAliasResolver {

    /** 고경합에서도 실무상 충분한 재시도 횟수. 무한 루프를 막는 상한이다. */
    private static final int MAX_ATTEMPTS = 5;

    private final CommunityAliasAllocator communityAliasAllocator;

    public CommunityAliasResolver(CommunityAliasAllocator communityAliasAllocator) {
        this.communityAliasAllocator = communityAliasAllocator;
    }

    public CommunityAlias resolve(UUID userId) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return communityAliasAllocator.allocate(userId);
            } catch (DataAccessException concurrentConflict) {
                // 유니크 제약 충돌(커밋 시점 포함). 다음 시도에서 재사용/번호 재계산으로 흡수된다.
            }
        }
        throw new BusinessException(
                ErrorCode.COMMON_CONFLICT, "익명 번호 발급이 반복 충돌했습니다. 잠시 후 다시 시도해 주세요.");
    }
}
