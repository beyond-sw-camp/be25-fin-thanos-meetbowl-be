package com.meetbowl.application.community;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.community.CommunityCommentListItem;

/**
 * 댓글 목록 한 줄의 결과다. 조회 읽기 모델({@link CommunityCommentListItem})에 작성자 "익명N" 표시명을 결합해 만든다. 실제 작성자
 * userId는 담지 않는다.
 *
 * <p>{@code mine}은 현재 사용자가 작성자인지, {@code liked}는 현재 사용자가 이 댓글에 좋아요를 눌렀는지다. 둘 다 boolean만 담아 익명성을 해치지
 * 않는다.
 */
public record CommentListItemResult(
        UUID id,
        String authorAlias,
        String content,
        long likeCount,
        Instant createdAt,
        boolean mine,
        boolean liked) {

    public static CommentListItemResult of(
            CommunityCommentListItem item, String authorAlias, boolean mine, boolean liked) {
        return new CommentListItemResult(
                item.id(),
                authorAlias,
                item.content(),
                item.likeCount(),
                item.createdAt(),
                mine,
                liked);
    }
}
