package com.meetbowl.domain.community;

/**
 * 게시글 목록 조회 조건이다.
 *
 * <p>{@code category}가 null이면 전체 카테고리, {@code keyword}가 null/공백이면 검색 미적용이다. {@code keyword}는 제목·내용에
 * 부분일치(대소문자 무시)로 매칭한다. {@code page}는 1부터 시작한다. 목록은 항상 최신순(createdAt DESC)이다.
 *
 * <p>{@code hotOnly}가 true면 "Hot 게시글" 목록(좋아요 {@link CommunityHotScore#HOT_LIKE_THRESHOLD}개 이상)으로
 * 한정한다. 이때도 category/keyword 검색은 함께 적용된다. 일반 목록은 {@code hotOnly=false}다.
 */
public record CommunityPostQuery(
        CommunityCategory category, String keyword, int page, int size, boolean hotOnly) {

    public CommunityPostQuery {
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 20;
        }
        // 공백뿐인 검색어는 미적용으로 정규화한다(빈 LIKE 패턴 방지).
        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }
    }

    /** 일반 목록 조회용(hotOnly=false) 하위호환 생성자. */
    public CommunityPostQuery(CommunityCategory category, String keyword, int page, int size) {
        this(category, keyword, page, size, false);
    }
}