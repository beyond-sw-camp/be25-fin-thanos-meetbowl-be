package com.meetbowl.domain.community;

import java.time.Instant;
import java.util.UUID;

/**
 * 댓글 목록 한 줄에 필요한 조회 전용 읽기 모델이다.
 *
 * <p>쓰기 모델 {@link Comment}는 작성 시각·좋아요 수를 들고 있지 않다(각각 BaseEntity·집계 책임). 목록 화면은 이 값들을 한 번의 조회로 함께
 * 내려줘야 하므로, 게시글 목록({@link CommunityPostListItem})과 같은 방식으로 조회 경계 전용 모델을 둔다.
 *
 * <p>{@code authorUserId}는 익명 별칭 매핑에만 쓰는 비공개 값이다. 응답으로는 노출하지 않으며 상위 계층이 {@link CommunityAlias}로
 * "익명N" 표시명을 붙인다.
 */
public record CommunityCommentListItem(
        UUID id, UUID authorUserId, String content, long likeCount, Instant createdAt) {}
