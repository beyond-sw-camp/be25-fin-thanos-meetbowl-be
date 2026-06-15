package com.meetbowl.application.community;

import java.util.UUID;

import com.meetbowl.domain.community.Comment;

/** 댓글 등록/수정 결과다. 작성자는 실제 userId 대신 "익명N" 표시명({@code authorAlias})으로만 노출한다. */
public record CommentResult(
        UUID id, UUID postId, String authorAlias, String content, long likeCount) {

    /** 등록 직후 결과. 좋아요 수는 아직 0이다. */
    public static CommentResult of(Comment comment, String authorAlias) {
        return of(comment, authorAlias, 0L);
    }

    /** 수정 결과처럼 좋아요 수가 이미 존재할 수 있는 경우. 카운트는 호출부에서 조회해 전달한다(수정으로 바뀌지 않는 읽기 값). */
    public static CommentResult of(Comment comment, String authorAlias, long likeCount) {
        return new CommentResult(
                comment.id(), comment.postId(), authorAlias, comment.content(), likeCount);
    }
}
