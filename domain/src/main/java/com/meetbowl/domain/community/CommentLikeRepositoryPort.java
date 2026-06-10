package com.meetbowl.domain.community;

import java.util.UUID;

/** 댓글 좋아요 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface CommentLikeRepositoryPort {

    CommentLike save(CommentLike commentLike);

    /* 사용자가 이 댓글에 이미 좋아요를 눌렀는지를 true/false로 확인하는 메서드
    댓글 한 건 + 사용자 한 명의 조합으로 좋아요 행이 있는지 보는 */
    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    /** 좋아요 취소(토글). */
    void deleteByCommentIdAndUserId(UUID commentId, UUID userId);

    /** 댓글의 좋아요 수. */
    long countByCommentId(UUID commentId);

    /** 댓글 삭제 시 좋아요 일괄 제거. */
    void deleteByCommentId(UUID commentId);
}