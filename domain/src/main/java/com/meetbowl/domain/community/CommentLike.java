package com.meetbowl.domain.community;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 댓글의 좋아요 도메인 모델이다(FR-123). (commentId, userId) 한 쌍이 "한 사용자가 한 댓글에 누른 좋아요" 1건을 표현한다. 중복 방지는 영속성의
 * 유니크 제약으로, 좋아요 수는 행 수 집계로, 취소는 행 삭제로 처리한다.
 */
public class CommentLike {

    private final UUID id;
    private final UUID commentId; // 어떤 댓글의 하트를 눌렀는지
    private final UUID userId; // 로그인한 사용자가 눌렀는지

    // commentId나 userId가 null로 들어올 때만 메세지가 출력되는 안전 장치용
    private CommentLike(UUID id, UUID commentId, UUID userId) {
        this.id = id;
        this.commentId = commentId;
        this.userId = userId;
    }

    public static CommentLike create(UUID commentId, UUID userId) {
        return of(null, commentId, userId);
    }

    public static CommentLike of(UUID id, UUID commentId, UUID userId) {
        if (commentId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "댓글은 필수입니다.");
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "사용자는 필수입니다.");
        }
        return new CommentLike(id, commentId, userId);
    }

    public UUID id() {
        return id;
    }

    public UUID commentId() {
        return commentId;
    }

    public UUID userId() {
        return userId;
    }
}
