package com.meetbowl.application.community;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.CommunityPostListItem;
import com.meetbowl.domain.community.CommunityPostQueryPort;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.PostLikeRepositoryPort;
import com.meetbowl.domain.community.PostRepositoryPort;

/**
 * 게시글 상세 조회 UseCase다. 조회 시 조회수(viewCount)를 1 증가시키고, 본문·좋아요수·댓글수·작성 시각을 함께 내린다.
 *
 * <p>흐름(한 트랜잭션): (1) 게시글을 불러와 없으면 404. (2) {@link Post#increaseViewCount} 로 조회수를 올려 저장한다. (3) 조회 전용
 * 포트로 단건을 다시 읽는다 — 같은 트랜잭션에서 저장이 먼저 flush 되므로 증가된 viewCount가 반영되고, 좋아요·댓글 수와 작성 시각도 한 번에 집계된다(쓰기 모델
 * {@link Post}에는 createdAt·카운트가 없기 때문). (4) 작성자 별칭과 현재 사용자의 좋아요 여부를 붙인다.
 *
 * <p>동시성: 조회수 증가는 읽고-더하고-쓰기라 동시 조회 시 일부 증가가 유실될 수 있으나(근사치 카운터), 조회수 특성상 허용한다. 정확한 누적이 필요하면 원자적 증가
 * UPDATE로 바꾼다.
 */
@Service
public class GetPostDetailUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final CommunityPostQueryPort communityPostQueryPort;
    private final PostLikeRepositoryPort postLikeRepositoryPort;
    private final CommunityAliasDisplayResolver aliasDisplayResolver;

    public GetPostDetailUseCase(
            PostRepositoryPort postRepositoryPort,
            CommunityPostQueryPort communityPostQueryPort,
            PostLikeRepositoryPort postLikeRepositoryPort,
            CommunityAliasDisplayResolver aliasDisplayResolver) {
        this.postRepositoryPort = postRepositoryPort;
        this.communityPostQueryPort = communityPostQueryPort;
        this.postLikeRepositoryPort = postLikeRepositoryPort;
        this.aliasDisplayResolver = aliasDisplayResolver;
    }

    @Transactional
    public PostDetailResult execute(UUID postId, UUID requesterId) {
        Post post =
                postRepositoryPort
                        .findById(postId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "게시글을 찾을 수 없습니다."));

        // 조회수 1 증가. 저장 후 아래 조회 전용 단건 읽기 시점에 flush 되어 증가값이 반영된다.
        postRepositoryPort.save(post.increaseViewCount());

        CommunityPostListItem detail =
                communityPostQueryPort
                        .findById(postId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "게시글을 찾을 수 없습니다."));

        String authorAlias =
                aliasDisplayResolver
                        .displayNames(Set.of(detail.authorUserId()))
                        .getOrDefault(
                                detail.authorUserId(),
                                CommunityAliasDisplayResolver.FALLBACK_DISPLAY_NAME);
        boolean liked = postLikeRepositoryPort.existsByPostIdAndUserId(postId, requesterId);
        boolean mine = detail.authorUserId().equals(requesterId);

        return PostDetailResult.of(detail, authorAlias, mine, liked);
    }
}
