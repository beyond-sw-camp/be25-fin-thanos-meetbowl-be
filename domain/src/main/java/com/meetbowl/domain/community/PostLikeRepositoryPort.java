package com.meetbowl.domain.community;

import java.util.UUID;

/** 게시글 좋아요 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface PostLikeRepositoryPort {

    PostLike save(PostLike postLike);

    boolean existsByPostIdAndUserId(
            UUID postId, UUID userId); // 사용자가 이 게시글에 이미 좋아요를 눌렀는지 true/false 확인

    /** 좋아요 취소(토글). */
    void deleteByPostIdAndUserId(UUID postId, UUID userId);

    /** 게시글의 좋아요 수. */
    long countByPostId(UUID postId);

    /** 게시글 삭제 시 좋아요 일괄 제거. */
    void deleteByPostId(UUID postId);
}
