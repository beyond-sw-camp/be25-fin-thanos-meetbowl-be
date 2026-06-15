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

/**
 * Hot 게시글 조회 UseCase다. 최근 {@link CommunityHotScore#HOT_WINDOW}(48시간) 내 작성 글 중 인기 점수 상위 {@link
 * CommunityHotScore#HOT_LIMIT}(3)개를 목록 상단 노출용으로 내린다.
 *
 * <p>인기순 목록(sort=popular, 전체 기간 페이징)과 성격이 다르다: Hot은 "최근 48시간 · 상위 3개"로 한정한다. 점수식은 동일하다.
 */
@Service
public class GetHotPostsUseCase {

    private final CommunityPostQueryPort communityPostQueryPort;
    private final CommunityAliasDisplayResolver aliasDisplayResolver;

    public GetHotPostsUseCase(
            CommunityPostQueryPort communityPostQueryPort,
            CommunityAliasDisplayResolver aliasDisplayResolver) {
        this.communityPostQueryPort = communityPostQueryPort;
        this.aliasDisplayResolver = aliasDisplayResolver;
    }

    @Transactional(readOnly = true)
    public List<PostListItemResult> execute() {
        Instant since = Instant.now().minus(CommunityHotScore.HOT_WINDOW);
        List<CommunityPostListItem> hotItems =
                communityPostQueryPort.findHot(since, CommunityHotScore.HOT_LIMIT);

        Set<UUID> authorUserIds =
                hotItems.stream()
                        .map(CommunityPostListItem::authorUserId)
                        .collect(Collectors.toSet());
        Map<UUID, String> aliasByUser = aliasDisplayResolver.displayNames(authorUserIds);

        return hotItems.stream()
                .map(
                        item ->
                                PostListItemResult.of(
                                        item,
                                        aliasByUser.getOrDefault(
                                                item.authorUserId(),
                                                CommunityAliasDisplayResolver
                                                        .FALLBACK_DISPLAY_NAME)))
                .toList();
    }
}
