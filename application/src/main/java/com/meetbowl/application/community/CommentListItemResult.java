package com.meetbowl.application.community;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.community.CommunityCommentListItem;

/**
 * 댓글 목록 한 줄의 결과다. 조회 읽기 모델({@link CommunityCommentListItem})에 작성자 "익명N" 표시명을 결합해 만든다. 실제 작성자
 * userId는 담지 않는다.
 */
public record CommentListItemResult(
        UUID id, String authorAlias, String content, long likeCount, Instant createdAt) {

    public static CommentListItemResult of(CommunityCommentListItem item, String authorAlias) {
        return new CommentListItemResult(
                item.id(), authorAlias, item.content(), item.likeCount(), item.createdAt());
    }
}
