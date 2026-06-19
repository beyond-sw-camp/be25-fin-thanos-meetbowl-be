package com.meetbowl.application.community;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.common.Paged;
import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.CommunityPostListItem;
import com.meetbowl.domain.community.CommunityPostQuery;
import com.meetbowl.domain.community.CommunityPostQueryPort;
import com.meetbowl.domain.community.CommunityPostSort;
import com.meetbowl.domain.community.PostLikeRepositoryPort;

/**
 * 게시글 목록 조회 UseCase다. 카테고리·검색어·정렬(최신순/인기순)·페이징 조건으로 한 페이지를 조회하고, 각 글의 작성자 userId를 "익명N" 표시명으로 배치
 * 변환해 내린다(실제 userId 비노출).
 *
 * <p>조회는 읽기 전용이다. 좋아요/댓글 수는 조회 시점 값을 읽기만 하고, 조회수(viewCount)도 목록에서는 증가시키지 않는다(조회수 증가는 상세 조회의 몫).
 */
@Service
public class ListPostUseCase {

    private final CommunityPostQueryPort communityPostQueryPort;
    private final CommunityAliasDisplayResolver aliasDisplayResolver;
    private final PostLikeRepositoryPort postLikeRepositoryPort;

    public ListPostUseCase(
            CommunityPostQueryPort communityPostQueryPort,
            CommunityAliasDisplayResolver aliasDisplayResolver,
            PostLikeRepositoryPort postLikeRepositoryPort) {
        this.communityPostQueryPort = communityPostQueryPort;
        this.aliasDisplayResolver = aliasDisplayResolver;
        this.postLikeRepositoryPort = postLikeRepositoryPort;
    }

    /**
     * 목록을 조회한다. app-api가 도메인 타입에 의존하지 않도록 카테고리/정렬은 문자열로 받아 여기서 해석한다. {@code category}가 null/공백이면 전체
     * 카테고리, {@code sort}가 "popular"면 인기순, 그 외(기본 latest)는 최신순이다.
     *
     * <p>{@code requesterId}는 현재 로그인 사용자다. 각 글의 작성자 여부(mine)·좋아요 여부(liked)를 계산해 내리되, 좋아요는 이 페이지의 글
     * ID를 모아 한 번에 배치 조회해 N+1을 피한다.
     */
    @Transactional(readOnly = true)
    public PostListPageResult execute(
            String category, String keyword, String sort, int page, int size, UUID requesterId) {
        CommunityCategory categoryFilter =
                (category == null || category.isBlank()) ? null : CommunityCategory.from(category);
        CommunityPostSort sortType =
                "popular".equalsIgnoreCase(sort == null ? "" : sort.trim())
                        ? CommunityPostSort.POPULAR
                        : CommunityPostSort.LATEST;
        CommunityPostQuery query =
                new CommunityPostQuery(categoryFilter, keyword, sortType, page, size);

        Paged<CommunityPostListItem> paged = communityPostQueryPort.search(query);

        // 이 페이지에 등장하는 작성자 userId를 한 번에 별칭으로 변환(N+1 회피).
        Set<UUID> authorUserIds =
                paged.content().stream()
                        .map(CommunityPostListItem::authorUserId)
                        .collect(Collectors.toSet());
        Map<UUID, String> aliasByUser = aliasDisplayResolver.displayNames(authorUserIds);

        // 이 페이지의 글 중 현재 사용자가 좋아요한 것들을 한 번에 배치 조회(N+1 회피).
        Set<UUID> postIds =
                paged.content().stream().map(CommunityPostListItem::id).collect(Collectors.toSet());
        Set<UUID> likedPostIds = postLikeRepositoryPort.findLikedPostIds(requesterId, postIds);

        var items =
                paged.content().stream()
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

        return new PostListPageResult(
                items,
                query.page(),
                query.size(),
                paged.totalElements(),
                totalPages(query.size(), paged.totalElements()));
    }

    private int totalPages(int size, long totalElements) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }
}
