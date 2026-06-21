package com.meetbowl.application.community;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.community.CommunityHotScore;
import com.meetbowl.domain.community.CommunityPostListItem;
import com.meetbowl.domain.community.CommunityPostQueryPort;
import com.meetbowl.domain.community.PostLikeRepositoryPort;

/**
 * Hot 게시글 조회 UseCase다. 최근 {@link CommunityHotScore#HOT_WINDOW}(24시간) 내에 작성 글 중 인기 점수 상위 {@link
 * CommunityHotScore#HOT_LIMIT}(4)개를 목록 상단 노출용으로 내린다.
 *
 * <p>인기순 목록(sort=popular, 전체 기간 페이징)과 성격이 다르다: Hot은 "최근 24시간 · 상위 4개"로 한정한다. 점수식은 동일하다.
 */
@Service
public class GetHotPostsUseCase {

    private final CommunityPostQueryPort communityPostQueryPort;
    private final CommunityAliasDisplayResolver aliasDisplayResolver;
    private final PostLikeRepositoryPort postLikeRepositoryPort;

    public GetHotPostsUseCase(
            CommunityPostQueryPort communityPostQueryPort,
            CommunityAliasDisplayResolver aliasDisplayResolver,
            PostLikeRepositoryPort postLikeRepositoryPort) {
        this.communityPostQueryPort = communityPostQueryPort;
        this.aliasDisplayResolver = aliasDisplayResolver;
        this.postLikeRepositoryPort = postLikeRepositoryPort;
    }

    /**
     * Hot 게시글을 조회한다. {@code requesterId}는 현재 로그인 사용자로, 각 글의 작성자 여부(mine)·좋아요 여부(liked)를 계산해 내린다.
     * 좋아요는 Hot 글 ID를 모아 한 번에 배치 조회해 N+1을 피한다.
     */
    @Transactional(readOnly = true)
    public List<PostListItemResult> execute(UUID requesterId) {
        Instant since = Instant.now().minus(CommunityHotScore.HOT_WINDOW);
        List<CommunityPostListItem> hotItems =
                communityPostQueryPort.findHot(since, CommunityHotScore.HOT_LIMIT);

        Set<UUID> authorUserIds =
                hotItems.stream()
                        .map(CommunityPostListItem::authorUserId)
                        .collect(Collectors.toSet());
        Map<UUID, String> aliasByUser = aliasDisplayResolver.displayNames(authorUserIds);

        // Hot 글 중 현재 사용자가 좋아요한 것들을 한 번에 배치 조회(N+1 회피).
        Set<UUID> postIds =
                hotItems.stream().map(CommunityPostListItem::id).collect(Collectors.toSet());
        Set<UUID> likedPostIds = postLikeRepositoryPort.findLikedPostIds(requesterId, postIds);

        return hotItems.stream()
                .map(
                        item ->
                                PostListItemResult.of(
                                        item,
                                        aliasByUser.getOrDefault(
                                                item.authorUserId(),
                                                CommunityAliasDisplayResolver
                                                        .FALLBACK_DISPLAY_NAME),
                                        item.authorUserId().equals(requesterId),
                                        likedPostIds.contains(item.id())))
                .toList();
    }
}
