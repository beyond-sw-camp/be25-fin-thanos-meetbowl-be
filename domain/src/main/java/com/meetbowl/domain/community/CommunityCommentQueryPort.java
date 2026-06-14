package com.meetbowl.domain.community;

import java.util.List;
import java.util.UUID;

/**
 * 커뮤니티 댓글 <b>조회 전용</b> 포트다. 댓글 목록은 좋아요 수 집계와 작성 시각이 필요하므로, 쓰기용 {@link CommentRepositoryPort}와 분리해 한
 * 번의 조회로 좋아요 수까지 모아 내린다(N+1 회피). 실제 구현은 infrastructure adapter가 담당한다.
 */
public interface CommunityCommentQueryPort {

    /** 한 게시글의 댓글 목록을 등록순(작성 시각 오름차순)으로, 각 댓글의 좋아요 수와 함께 조회한다. */
    List<CommunityCommentListItem> findByPostId(UUID postId);
}
