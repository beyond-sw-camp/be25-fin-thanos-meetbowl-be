package com.meetbowl.api.community;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.community.PostListItemResult;

/**
 * 게시글 목록/Hot 한 줄 응답이다. 작성자는 "익명N"({@code authorAlias})만 노출하고 실제 userId는 담지 않는다. {@code mine}은 현재
 * 사용자가 작성자인지(수정/삭제 버튼 노출용), {@code liked}는 현재 사용자가 좋아요를 눌렀는지(하트 상태용)다. {@code createdAt}은 ISO-8601
 * UTC로 직렬화된다.
 */
public record PostListItemResponse(
        UUID id,
        String category,
        String categoryLabel,
        String title,
        String content,
        String authorAlias,
        Instant createdAt,
        long viewCount,
        long likeCount,
        long commentCount,
        boolean mine,
        boolean liked) {

    public static PostListItemResponse from(PostListItemResult result) {
        return new PostListItemResponse(
                result.id(),
                result.category(),
                result.categoryLabel(),
                result.title(),
                result.content(),
                result.authorAlias(),
                result.createdAt(),
                result.viewCount(),
                result.likeCount(),
                result.commentCount(),
                result.mine(),
                result.liked());
    }
}
