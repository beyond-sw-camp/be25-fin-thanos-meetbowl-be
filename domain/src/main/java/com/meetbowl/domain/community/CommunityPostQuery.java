package com.meetbowl.domain.community;

/**
 * 게시글 목록 조회 조건이다.
 *
 * <p>{@code category}가 null이면 전체 카테고리, {@code keyword}가 null/공백이면 검색 미적용이다. {@code keyword}는 제목·내용에
 * 부분일치(대소문자 무시)로 매칭한다. {@code page}는 1부터 시작한다. 정렬은 {@link CommunityPostSort}를 따른다.
 */
public record CommunityPostQuery(
        CommunityCategory category, String keyword, CommunityPostSort sort, int page, int size) {

    public CommunityPostQuery {
        if (sort == null) {
            sort = CommunityPostSort.LATEST;
        }
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
}
