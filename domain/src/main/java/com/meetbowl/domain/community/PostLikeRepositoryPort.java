package com.meetbowl.domain.community;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/** 게시글 좋아요 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface PostLikeRepositoryPort {

    PostLike save(PostLike postLike);

    boolean existsByPostIdAndUserId(
            UUID postId, UUID userId); // 사용자가 이 게시글에 이미 좋아요를 눌렀는지 true/false 확인

    /**
     * 사용자가 좋아요 누른 게시글 ID들을 한 번에 조회한다(목록 N+1 회피). {@code postIds} 중 {@code userId}가 좋아요한 것만 골라
     * 반환하므로, 목록의 글 ID 집합을 넘기면 좋아요 여부를 쿼리 한 번으로 매핑할 수 있다.
     */
    Set<UUID> findLikedPostIds(UUID userId, Collection<UUID> postIds);

    /** 좋아요 취소(토글). */
    void deleteByPostIdAndUserId(UUID postId, UUID userId);

    /** 게시글의 좋아요 수. */
    long countByPostId(UUID postId);

    /** 게시글 삭제 시 좋아요 일괄 제거. */
    void deleteByPostId(UUID postId);
}
