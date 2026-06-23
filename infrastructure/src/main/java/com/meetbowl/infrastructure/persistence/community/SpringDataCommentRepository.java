package com.meetbowl.infrastructure.persistence.community;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.meetbowl.domain.community.CommunityCommentListItem;

/** Spring Data JPA 전용 repository다. application/domain 계층에서는 이 타입을 직접 참조하지 않는다. */
public interface SpringDataCommentRepository extends JpaRepository<CommentEntity, UUID> {

    List<CommentEntity> findByPostIdOrderByCreatedAtAsc(UUID postId);

    long countByPostId(UUID postId);

    void deleteByPostId(UUID postId);

    /*
     * 댓글 목록용 프로젝션 쿼리다. 댓글마다 좋아요 수를 함께 집계해 읽기 모델로 바로 매핑한다.
     * - 좋아요는 댓글과 연관 매핑이 없고 commentId(raw UUID)로만 참조하므로 Hibernate 엔티티 ON 조인으로 LEFT JOIN 한다.
     * - 좋아요가 없을 수 있으니 LEFT JOIN + COUNT(DISTINCT)로 0건도 포함해 센다.
     * - 등록순(작성 시각 오름차순)으로 정렬한다.
     */
    @Query(
            "SELECT new com.meetbowl.domain.community.CommunityCommentListItem("
                    + " c.id, c.authorUserId, c.content, COUNT(DISTINCT cl.id), c.createdAt)"
                    + " FROM CommentEntity c"
                    + " LEFT JOIN CommentLikeEntity cl ON cl.commentId = c.id"
                    + " WHERE c.postId = :postId"
                    + " GROUP BY c.id, c.authorUserId, c.content, c.createdAt"
                    + " ORDER BY c.createdAt ASC")
    List<CommunityCommentListItem> findCommentListByPostId(@Param("postId") UUID postId);
}
