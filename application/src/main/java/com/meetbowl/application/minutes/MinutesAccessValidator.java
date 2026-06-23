package com.meetbowl.application.minutes;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.minutes.Minutes;

/** 회원/회의 테이블이 없는 현재 단계에서 JWT 조직과 회의록 조직의 경계를 검증한다. */
final class MinutesAccessValidator {

    private MinutesAccessValidator() {}

    /**
     * 검토자 ID 일치만 확인하면 다른 조직의 동일 사용 주체가 잘못 접근하는 경계를 놓칠 수 있다.
     *
     * <p>회원·회의 관계가 구현되면 해당 권한 Port로 교체하되, 그 전까지는 JWT organizationId와 회의록 organizationId를 비교한다.
     */
    static void ensureSameOrganization(Minutes minutes, UUID actorOrganizationId) {
        if (actorOrganizationId == null || !minutes.organizationId().equals(actorOrganizationId)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "다른 조직의 회의록은 처리할 수 없습니다.");
        }
    }
}
