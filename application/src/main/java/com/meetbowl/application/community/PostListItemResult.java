package com.meetbowl.application.community;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.community.CommunityPostListItem;

/**
 * 게시글 목록/Hot 한 줄의 결과다. 조회 읽기 모델({@link CommunityPostListItem})에 작성자 "익명N" 표시명을 결합해 만든다. 실제 작성자
 * userId는 담지 않는다.
 *
 * <p>카테고리는 app-api가 도메인 enum에 의존하지 않도록 코드값({@code category})과 한글 라벨({@code categoryLabel}) 문자열로 풀어
 * 담는다.
 *
 * <p>{@code mine}은 현재 사용자가 작성자인지, {@code liked}는 현재 사용자가 이 글에 좋아요를 눌렀는지다. 둘 다 boolean만 담아 익명성을 해치지
 * 않는다(작성자 userId는 여전히 비노출).
 */
public record PostListItemResult(
        UUID id,
        String category,
        String categoryLabel,
        String title,
        String content,
        String authorAlias,
        Instant createdAt,
        long viewCount,
        long likeCount,
        long commentCount,
        boolean mine,
        boolean liked) {

    public static PostListItemResult of(
            CommunityPostListItem item, String authorAlias, boolean mine, boolean liked) {
        return new PostListItemResult(
                item.id(),
                item.category().name(),
                item.category().label(),
                item.title(),
                item.content(),
                authorAlias,
                item.createdAt(),
                item.viewCount(),
                item.likeCount(),
                item.commentCount(),
                mine,
                liked);
    }
}
