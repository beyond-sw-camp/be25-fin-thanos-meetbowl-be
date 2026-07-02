package com.meetbowl.application.community;

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
import com.meetbowl.domain.community.PostLikeRepositoryPort;

/**
 * 게시글 목록 조회 UseCase다. 카테고리·검색어·페이징 조건으로 한 페이지를 최신순으로 조회하고, 각 글의 작성자 userId를 "익명N" 표시명으로 배치 변환해
 * 내린다(실제 userId 비노출).
 *
 * <p>조회는 읽기 전용이다. 좋아요/댓글 수는 조회 시점 값을 읽기만 하고, 조회수(viewCount)도 목록에서는 증가시키지 않는다(조회수 증가는 상세 조회의 몫).
 */
@Service
public class ListPostUseCase {

    private final CommunityPostQueryPort communityPostQueryPort;
    private final CommunityAliasPolicy communityAliasPolicy;
    private final PostLikeRepositoryPort postLikeRepositoryPort;

    public ListPostUseCase(
            CommunityPostQueryPort communityPostQueryPort,
            CommunityAliasPolicy communityAliasPolicy,
            PostLikeRepositoryPort postLikeRepositoryPort) {
        this.communityPostQueryPort = communityPostQueryPort;
        this.communityAliasPolicy = communityAliasPolicy;
        this.postLikeRepositoryPort = postLikeRepositoryPort;
    }

    /**
     * 목록을 조회한다. app-api가 도메인 타입에 의존하지 않도록 카테고리는 문자열로 받아 여기서 해석한다. {@code category}가 null/공백이면 전체
     * 카테고리다. 목록은 항상 최신순이다.
     *
     * <p>{@code requesterId}는 현재 로그인 사용자다. 각 글의 작성자 여부(mine)·좋아요 여부(liked)를 계산해 내리되, 좋아요는 이 페이지의 글
     * ID를 모아 한 번에 배치 조회해 N+1을 피한다. 커뮤니티는 로그인 전용이라 정상 흐름에서 null이 아니지만, 방어적으로 null이면 배치 조회를 생략하고
     * mine·liked를 모두 false로 내린다(NPE 방지).
     *
     * <p>{@code hot}이 true면 "Hot 게시글"(좋아요 {@link
     * com.meetbowl.domain.community.CommunityHotScore#HOT_LIKE_THRESHOLD}개 이상) 목록으로 한정한다.
     * category/keyword 검색은 함께 적용된다.
     */
    @Transactional(readOnly = true)
    public PostListPageResult execute(
            String category, String keyword, int page, int size, boolean hot, UUID requesterId) {
        CommunityCategory categoryFilter =
                (category == null || category.isBlank()) ? null : CommunityCategory.from(category);
        CommunityPostQuery query = new CommunityPostQuery(categoryFilter, keyword, page, size, hot);

        Paged<CommunityPostListItem> paged = communityPostQueryPort.search(query);

        // 이 페이지의 글 중 현재 사용자가 좋아요한 것들을 한 번에 배치 조회(N+1 회피). 비로그인(null)이면 조회 생략 → 빈 집합.
        Set<UUID> postIds =
                paged.content().stream().map(CommunityPostListItem::id).collect(Collectors.toSet());
        Set<UUID> likedPostIds =
                requesterId == null
                        ? Set.of()
                        : postLikeRepositoryPort.findLikedPostIds(requesterId, postIds);

        var items =
                paged.content().stream()
                        .map(
                                item ->
                                        PostListItemResult.of(
                                                item,
                                                communityAliasPolicy.postAuthorAlias(),
                                                requesterId != null
                                                        && item.authorUserId().equals(requesterId),
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
