package com.meetbowl.domain.community;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 커뮤니티 익명 표시명 매핑 도메인 모델이다(FR-119).
 *
 * <p>한 사용자에게 전역적으로 고정된 익명 번호({@code aliasNo})를 부여한다. 첫 작성(글이든 댓글이든) 시 다음 번호를 발급받고, 이후 모든 글·댓글에서 같은
 * 번호를 재사용한다. 화면 표시명은 {@code "익명" + aliasNo}다. (userId·aliasNo는 각각 유니크)
 */
public class CommunityAlias {

    private final UUID id;

    /** 대상 사용자(FK, 유니크). */
    private final UUID userId; // 누구의 익명인지

    /** 전역 익명 번호(1 이상, 유니크). */
    private final int aliasNo; // 그 번호

    private CommunityAlias(UUID id, UUID userId, int aliasNo) {
        this.id = id;
        this.userId = userId;
        this.aliasNo = aliasNo;
    }

    public static CommunityAlias create(UUID userId, int aliasNo) {
        return of(null, userId, aliasNo);
    }

    // 0이나 음수 방지용
    public static CommunityAlias of(UUID id, UUID userId, int aliasNo) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "사용자는 필수입니다.");
        }
        if (aliasNo < 1) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "익명 번호는 1 이상이어야 합니다.");
        }
        return new CommunityAlias(id, userId, aliasNo);
    }

    /** 화면 표시명(예: "익명1"). */
    public String displayName() {
        return "익명" + aliasNo;
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public int aliasNo() {
        return aliasNo;
    }
}
