package com.meetbowl.application.community;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.community.CommunityPostListItem;

/**
 * 게시글 상세 결과다. 작성자는 실제 userId 대신 "익명N"({@code authorAlias})만 노출한다. 목록 항목에 더해 본문 전체와 조회 시점 카운트를 담고,
 * 현재 사용자가 이 글에 좋아요를 눌렀는지({@code liked})를 함께 내려 상세 화면의 하트 상태를 표시할 수 있게 한다.
 *
 * <p>{@code viewCount}는 이번 조회로 1 증가된 값이다.
 */
public record PostDetailResult(
        UUID id,
        String category,
        String categoryLabel,
        String authorAlias,
        String title,
        String content,
        Instant createdAt,
        long viewCount,
        long likeCount,
        long commentCount,
        boolean liked) {

    public static PostDetailResult of(
            CommunityPostListItem item, String authorAlias, boolean liked) {
        return new PostDetailResult(
                item.id(),
                item.category().name(),
                item.category().label(),
                authorAlias,
                item.title(),
                item.content(),
                item.createdAt(),
                item.viewCount(),
                item.likeCount(),
                item.commentCount(),
                liked);
    }
}
