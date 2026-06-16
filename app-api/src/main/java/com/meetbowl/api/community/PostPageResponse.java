package com.meetbowl.api.community;

import java.util.List;

import com.meetbowl.application.community.PostListPageResult;

/** 게시글 목록 페이지 응답 본문이다. conventions의 목록 포맷(items + page/size/totalElements/totalPages)을 따른다. */
public record PostPageResponse(
        List<PostListItemResponse> items, int page, int size, long totalElements, int totalPages) {

    public static PostPageResponse from(PostListPageResult result) {
        return new PostPageResponse(
                result.items().stream().map(PostListItemResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
