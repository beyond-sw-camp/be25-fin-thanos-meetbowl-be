package com.meetbowl.domain.community;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/** 댓글 좋아요 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface CommentLikeRepositoryPort {

    CommentLike save(CommentLike commentLike);

    /* 사용자가 이 댓글에 이미 좋아요를 눌렀는지를 true/false로 확인하는 메서드
    댓글 한 건 + 사용자 한 명의 조합으로 좋아요 행이 있는지 보는 */
    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    /**
     * 사용자가 좋아요 누른 댓글 ID들을 한 번에 조회한다(목록 N+1 회피). {@code commentIds} 중 {@code userId}가 좋아요한 것만 골라
     * 반환하므로, 댓글 목록의 ID 집합을 넘기면 좋아요 여부를 쿼리 한 번으로 매핑할 수 있다.
     */
    Set<UUID> findLikedCommentIds(UUID userId, Collection<UUID> commentIds);

    /** 좋아요 취소(토글). */
    void deleteByCommentIdAndUserId(UUID commentId, UUID userId);

    /** 댓글의 좋아요 수. */
    long countByCommentId(UUID commentId);

    /** 댓글 삭제 시 좋아요 일괄 제거. */
    void deleteByCommentId(UUID commentId);
}
