package com.meetbowl.api.community;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.community.CommentListItemResult;

/**
 * 댓글 목록 한 줄 응답이다. 작성자는 "익명N"({@code authorAlias})만 노출하고 실제 userId는 담지 않는다. {@code createdAt}은
 * ISO-8601 UTC로 직렬화된다.
 */
public record CommentListItemResponse(
        UUID id, String authorAlias, String content, long likeCount, Instant createdAt) {

    public static CommentListItemResponse from(CommentListItemResult result) {
        return new CommentListItemResponse(
                result.id(),
                result.authorAlias(),
                result.content(),
                result.likeCount(),
                result.createdAt());
    }
}
