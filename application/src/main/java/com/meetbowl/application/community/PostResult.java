package com.meetbowl.application.community;

import java.util.UUID;

import com.meetbowl.domain.community.Post;

/**
 * 게시글 등록/수정 결과다. 작성자는 실제 userId 대신 "익명N" 표시명({@code authorAlias})으로만 노출한다.
 *
 * <p>카테고리는 app-api가 도메인 enum에 의존하지 않도록 코드값({@code category}, enum 이름)과 화면 표시용 한글 라벨({@code
 * categoryLabel}) 문자열로 풀어 담는다.
 */
public record PostResult(
        UUID id,
        String category,
        String categoryLabel,
        String authorAlias,
        String title,
        String content,
        long viewCount,
        long likeCount,
        long commentCount) {

    /** 등록 직후 결과. 좋아요/댓글 수는 아직 0이다. */
    public static PostResult of(Post post, String authorAlias) {
        return of(post, authorAlias, 0L, 0L);
    }

    /** 수정 결과처럼 좋아요/댓글 수가 이미 존재할 수 있는 경우. 카운트는 호출부에서 조회해 전달한다(수정으로 바뀌지 않는 읽기 값). */
    public static PostResult of(Post post, String authorAlias, long likeCount, long commentCount) {
        return new PostResult(
                post.id(),
                post.category().name(),
                post.category().label(),
                authorAlias,
                post.title(),
                post.content(),
                post.viewCount(),
                likeCount,
                commentCount);
    }
}
