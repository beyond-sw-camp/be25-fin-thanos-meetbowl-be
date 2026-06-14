package com.meetbowl.domain.community;

/**
 * 게시글 목록 정렬 기준이다.
 *
 * <p>{@link #LATEST}는 작성 시각(createdAt) 내림차순이다. {@link #POPULAR}는 인기 점수({@link CommunityHotScore})
 * 내림차순이며, Hot 게시글과 달리 <b>전체 기간</b>을 대상으로 페이징한다. (Hot은 최근 48시간 상위 3개로 성격이 다르다 — {@link
 * CommunityHotScore} 주석 참고.)
 */
public enum CommunityPostSort {
    LATEST,
    POPULAR
}
