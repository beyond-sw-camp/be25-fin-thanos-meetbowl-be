package com.meetbowl.application.community;

import java.util.List;

/** 게시글 목록 한 페이지다. 항목과 페이지 메타(현재 페이지·크기·전체 개수·전체 페이지 수)를 담는다. page는 1부터 시작한다. */
public record PostListPageResult(
        List<PostListItemResult> items, int page, int size, long totalElements, int totalPages) {}
