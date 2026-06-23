package com.meetbowl.infrastructure.persistence.community;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data JPA 전용 repository다. application/domain 계층에서는 이 타입을 직접 참조하지 않는다. */
public interface SpringDataCommentLikeRepository extends JpaRepository<CommentLikeEntity, UUID> {

    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    /** 사용자가 좋아요한 댓글 ID만 골라 반환(목록 좋아요 여부 배치 조회). */
    @Query(
            "select cl.commentId from CommentLikeEntity cl"
                    + " where cl.userId = :userId and cl.commentId in :commentIds")
    List<UUID> findLikedCommentIds(
            @Param("userId") UUID userId, @Param("commentIds") Collection<UUID> commentIds);

    void deleteByCommentIdAndUserId(UUID commentId, UUID userId);

    long countByCommentId(UUID commentId);

    void deleteByCommentId(UUID commentId);
}
