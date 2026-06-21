package com.meetbowl.domain.community;

import java.time.Duration;

/**
 * 커뮤니티 Hot 점수 규칙을 한 곳에 모은 도메인 상수다.
 *
 * <p>점수식: {@code viewCount * 0.1 + likeCount * 2 + commentCount * 3}. 댓글이 좋아요보다, 좋아요가 조회보다 더 큰 참여로
 * 가중된다.
 *
 * <ul>
 *   <li>Hot: 최근 {@link #HOT_WINDOW}(24h) 내에 작성 글 중 점수 상위 {@link #HOT_LIMIT}(4)개. 목록 상단 노출용
 * </ul>
 *
 * <p>주의: 정렬을 DB에서 수행하는 조회 어댑터(JPQL/SQL)는 페이징 정합성을 위해 같은 가중치 숫자(0.1/2/3)를 쿼리에 직접 사용한다. 가중치를 바꿀 때는 이
 * 클래스와 해당 쿼리를 함께 수정한다(쿼리 주석에 본 클래스를 참조해 둔다).
 */
public final class CommunityHotScore {

    /** 조회수 가중치. */
    public static final double VIEW_WEIGHT = 0.1;

    /** 좋아요 가중치. */
    public static final double LIKE_WEIGHT = 2.0;

    /** 댓글 가중치. */
    public static final double COMMENT_WEIGHT = 3.0;

    /** Hot 대상 기간(최근 24시간). */
    public static final Duration HOT_WINDOW = Duration.ofHours(24);

    /** Hot 반환 개수(상위 4개). */
    public static final int HOT_LIMIT = 4;

    /**
     * "Hot 게시글" 목록(F-커뮤니티)의 좋아요 임계값이다. 좋아요 수가 이 값 이상인 게시글을 Hot으로 본다. 캐러셀용 {@link #HOT_LIMIT}(상위
     * N개)과는 별개의 기준으로, 점수가 아닌 좋아요 절대 개수로 필터링한다. 기준을 바꾸려면 이 상수만 수정하면 된다.
     */
    public static final int HOT_LIKE_THRESHOLD = 3;

    private CommunityHotScore() {}

    /** 참여 지표로 인기 점수를 계산한다. 인메모리 계산/테스트용 단일 진실 공급원이다. */
    public static double score(long viewCount, long likeCount, long commentCount) {
        return viewCount * VIEW_WEIGHT + likeCount * LIKE_WEIGHT + commentCount * COMMENT_WEIGHT;
    }
}
