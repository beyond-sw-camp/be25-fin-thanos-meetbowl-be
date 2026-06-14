package com.meetbowl.domain.community;

import java.time.Duration;

/**
 * 커뮤니티 인기/Hot 점수 규칙을 한 곳에 모은 도메인 상수다.
 *
 * <p>점수식: {@code viewCount * 0.1 + likeCount * 2 + commentCount * 3}. 댓글이 좋아요보다, 좋아요가 조회보다 더 큰 참여로
 * 가중된다.
 *
 * <p><b>Hot 과 인기순(sort=popular)의 구분</b> — 점수식은 같지만 적용 범위가 다르다.
 *
 * <ul>
 *   <li>Hot: 최근 {@link #HOT_WINDOW}(48h) 내 작성 글 중 점수 상위 {@link #HOT_LIMIT}(3)개. 목록 상단 노출용.
 *   <li>인기순: 전체 기간 글을 점수 내림차순으로 정렬해 페이징.
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

    /** Hot 대상 기간(최근 48시간). */
    public static final Duration HOT_WINDOW = Duration.ofHours(48);

    /** Hot 반환 개수(상위 3개). */
    public static final int HOT_LIMIT = 3;

    private CommunityHotScore() {}

    /** 참여 지표로 인기 점수를 계산한다. 인메모리 계산/테스트용 단일 진실 공급원이다. */
    public static double score(long viewCount, long likeCount, long commentCount) {
        return viewCount * VIEW_WEIGHT + likeCount * LIKE_WEIGHT + commentCount * COMMENT_WEIGHT;
    }
}
