package com.meetbowl.api.community;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.community.PostDetailResult;

/**
 * 게시글 상세 응답이다. 작성자는 "익명N"({@code authorAlias})만 노출한다. {@code viewCount}는 이번 조회로 1 증가된 값이고, {@code
 * mine}은 현재 사용자가 작성자인지(수정/삭제 버튼 노출용), {@code liked}는 현재 사용자의 좋아요 여부(하트 상태용)다. {@code createdAt}은
 * ISO-8601 UTC로 직렬화된다.
 */
public record PostDetailResponse(
        UUID id,
        String category,
        String categoryLabel,
        String authorAlias,
        String title,
        String content,
        Instant createdAt,
        long viewCount,
        long likeCount,
        long commentCount,
        boolean mine,
        boolean liked) {

    public static PostDetailResponse from(PostDetailResult result) {
        return new PostDetailResponse(
                result.id(),
                result.category(),
                result.categoryLabel(),
                result.authorAlias(),
                result.title(),
                result.content(),
                result.createdAt(),
                result.viewCount(),
                result.likeCount(),
                result.commentCount(),
                result.mine(),
                result.liked());
    }
}
