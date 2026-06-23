package com.meetbowl.infrastructure.persistence.community;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data JPA 전용 repository다. application/domain 계층에서는 이 타입을 직접 참조하지 않는다. */
public interface SpringDataPostLikeRepository extends JpaRepository<PostLikeEntity, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    /** 사용자가 좋아요한 게시글 ID만 골라 반환(목록 좋아요 여부 배치 조회). */
    @Query(
            "select pl.postId from PostLikeEntity pl"
                    + " where pl.userId = :userId and pl.postId in :postIds")
    List<UUID> findLikedPostIds(
            @Param("userId") UUID userId, @Param("postIds") Collection<UUID> postIds);

    void deleteByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);

    void deleteByPostId(UUID postId);
}
