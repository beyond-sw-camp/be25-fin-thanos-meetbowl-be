package com.meetbowl.domain.community;

import java.time.Instant;
import java.util.UUID;

/**
 * 커뮤니티 게시글 목록 한 줄에 필요한 조회 전용 읽기 모델이다.
 *
 * <p>쓰기 모델인 {@link Post}는 작성 시각·좋아요 수·댓글 수를 들고 있지 않다(각각 BaseEntity·집계 책임). 목록/Hot 화면은 이 값들을 한 번의
 * 조회로 함께 내려줘야 하고, 인기순은 점수로 DB 정렬·페이징해야 하므로, 도메인 모델을 건드리지 않고 조회 경계 전용 모델을 둔다(CQRS의 읽기 측).
 *
 * <p>{@code authorUserId}는 작성자 식별·익명 별칭 매핑에만 쓰는 비공개 값이다. 응답으로는 절대 노출하지 않으며, 상위 계층이 {@link
 * CommunityAlias}로 "익명N" 표시명을 붙여 가공한다.
 */
public record CommunityPostListItem(
        UUID id,
        CommunityCategory category,
        String title,
        String content,
        UUID authorUserId,
        long viewCount,
        long likeCount,
        long commentCount,
        Instant createdAt) {

    /** 인기/Hot 정렬용 점수. 인메모리 정렬(Hot 검증 등)에 사용한다. */
    public double score() {
        return CommunityHotScore.score(viewCount, likeCount, commentCount);
    }
}
