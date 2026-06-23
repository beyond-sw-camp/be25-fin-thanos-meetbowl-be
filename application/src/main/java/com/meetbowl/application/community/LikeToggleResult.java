package com.meetbowl.application.community;

/**
 * 좋아요 토글 결과다. {@code liked}는 토글 후 현재 상태(true=좋아요됨, false=취소됨), {@code likeCount}는 토글 후 집계된 좋아요 수다.
 * 화면이 하트 상태와 카운트를 즉시 갱신할 수 있게 둘을 함께 내린다.
 */
public record LikeToggleResult(boolean liked, long likeCount) {}
