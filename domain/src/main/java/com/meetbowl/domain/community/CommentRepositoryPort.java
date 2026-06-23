package com.meetbowl.domain.community;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 댓글 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface CommentRepositoryPort {

    Comment save(Comment comment);

    Optional<Comment> findById(UUID id);

    /** 한 게시글의 댓글 목록(등록순). */
    List<Comment> findByPostId(UUID postId);

    /** 게시글의 댓글 수(목록 표시용). */
    long countByPostId(UUID postId);

    void deleteById(UUID id);

    /** 게시글 삭제 시 댓글 일괄 제거. */
    void deleteByPostId(UUID postId);
}
