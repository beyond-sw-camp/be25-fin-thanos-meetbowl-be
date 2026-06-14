package com.meetbowl.api.community;

import java.util.UUID;

import com.meetbowl.application.community.PostResult;

/**
 * 게시글 등록 응답 본문이다. 작성자는 실제 userId 대신 "익명N"({@code authorAlias})만 노출한다. {@code category}는 코드값(enum
 * 이름), {@code categoryLabel}은 화면 표시용 한글 라벨이다.
 */
public record PostResponse(
        UUID id,
        String category,
        String categoryLabel,
        String authorAlias,
        String title,
        String content,
        long viewCount,
        long likeCount,
        long commentCount) {

    public static PostResponse from(PostResult result) {
        return new PostResponse(
                result.id(),
                result.category(),
                result.categoryLabel(),
                result.authorAlias(),
                result.title(),
                result.content(),
                result.viewCount(),
                result.likeCount(),
                result.commentCount());
    }
}
