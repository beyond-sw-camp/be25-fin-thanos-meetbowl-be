package com.meetbowl.api.community;

import java.util.UUID;

import com.meetbowl.application.community.CommentResult;

/** 댓글 응답 본문이다. 작성자는 실제 userId 대신 "익명N"({@code authorAlias})만 노출한다. */
public record CommentResponse(
        UUID id, UUID postId, String authorAlias, String content, long likeCount) {

    public static CommentResponse from(CommentResult result) {
        return new CommentResponse(
                result.id(),
                result.postId(),
                result.authorAlias(),
                result.content(),
                result.likeCount());
    }
}
