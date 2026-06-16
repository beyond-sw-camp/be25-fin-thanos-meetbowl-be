package com.meetbowl.api.community;

import com.meetbowl.application.community.LikeToggleResult;

/**
 * 좋아요 토글 응답 본문이다. {@code liked}는 토글 후 현재 상태(true=좋아요됨, false=취소됨), {@code likeCount}는 토글 후 좋아요 수다.
 */
public record LikeResponse(boolean liked, long likeCount) {

    public static LikeResponse from(LikeToggleResult result) {
        return new LikeResponse(result.liked(), result.likeCount());
    }
}
