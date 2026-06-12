package com.meetbowl.domain.community;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 게시글의 좋아요 도메인 모델이다(FR-123). (postId, userId) 한 쌍이 "한 사용자가 한 게시글에 누른 좋아요" 1건을 표현한다. 중복 방지는 영속성의 유니크
 * 제약으로, 좋아요 수는 행 수 집계로, 취소는 행 삭제로 처리한다.
 */
public class PostLike {

    private final UUID id;
    private final UUID postId;
    private final UUID userId;

    private PostLike(UUID id, UUID postId, UUID userId) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
    }

    public static PostLike create(UUID postId, UUID userId) {
        return of(null, postId, userId);
    }

    // 안전 장치용
    public static PostLike of(UUID id, UUID postId, UUID userId) {
        if (postId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "게시글은 필수입니다.");
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "사용자는 필수입니다.");
        }
        return new PostLike(id, postId, userId);
    }

    public UUID id() {
        return id;
    }

    public UUID postId() {
        return postId;
    }

    public UUID userId() {
        return userId;
    }
}
