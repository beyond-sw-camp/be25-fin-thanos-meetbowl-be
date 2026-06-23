package com.meetbowl.domain.community;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.meetbowl.domain.common.Paged;

/**
 * 커뮤니티 게시글 <b>조회 전용</b> 포트다. 목록/Hot/상세 화면은 좋아요·댓글 수 집계와 작성 시각이 필요하므로, 쓰기용 {@link
 * PostRepositoryPort}와 분리해 한 번의 조회로 카운트·작성 시각까지 모아 내린다(N+1 회피). 실제 구현은 infrastructure adapter가
 * 담당한다.
 */
public interface CommunityPostQueryPort {

    /** 조건(카테고리·검색어·정렬)에 맞는 게시글 목록 한 페이지를 좋아요·댓글 수와 함께 조회한다. */
    Paged<CommunityPostListItem> search(CommunityPostQuery query);

    /** 게시글 단건을 좋아요·댓글 수, 작성 시각과 함께 조회한다(상세 화면용). 없으면 빈 값. */
    Optional<CommunityPostListItem> findById(UUID postId);

    /**
     * {@code since} 이후 작성된 게시글 중 인기 점수 상위 {@code limit}개를 조회한다. Hot 섹션 전용으로, 점수 동률 시 최신 글을 우선한다.
     */
    List<CommunityPostListItem> findHot(Instant since, int limit);
}
