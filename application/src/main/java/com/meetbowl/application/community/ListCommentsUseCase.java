package com.meetbowl.application.community;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.CommentLikeRepositoryPort;
import com.meetbowl.domain.community.CommunityCommentListItem;
import com.meetbowl.domain.community.CommunityCommentQueryPort;
import com.meetbowl.domain.community.PostRepositoryPort;

/**
 * 댓글 목록 조회 UseCase다. 한 게시글의 댓글을 등록순으로, 각 댓글의 좋아요 수와 함께 조회하고, 작성자 userId를 "익명N" 표시명으로 배치 변환해 내린다(실제
 * userId 비노출).
 *
 * <p>게시글이 없거나 삭제됐으면 404로 거부한다(게시글 삭제 시 댓글은 cascade로 함께 제거되므로, 존재하지 않는 게시글의 댓글을 조회할 일은 없다). 조회는 읽기
 * 전용으로, 좋아요 수는 조회 시점 값을 읽기만 한다.
 */
@Service
public class ListCommentsUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final CommunityCommentQueryPort communityCommentQueryPort;
    private final CommunityAliasDisplayResolver aliasDisplayResolver;
    private final CommentLikeRepositoryPort commentLikeRepositoryPort;

    public ListCommentsUseCase(
            PostRepositoryPort postRepositoryPort,
            CommunityCommentQueryPort communityCommentQueryPort,
            CommunityAliasDisplayResolver aliasDisplayResolver,
            CommentLikeRepositoryPort commentLikeRepositoryPort) {
        this.postRepositoryPort = postRepositoryPort;
        this.communityCommentQueryPort = communityCommentQueryPort;
        this.aliasDisplayResolver = aliasDisplayResolver;
        this.commentLikeRepositoryPort = commentLikeRepositoryPort;
    }

    /**
     * 한 게시글의 댓글 목록을 조회한다. {@code requesterId}는 현재 로그인 사용자로, 각 댓글의 작성자 여부(mine)·좋아요 여부(liked)를 계산해
     * 내린다. 좋아요는 댓글 ID를 모아 한 번에 배치 조회해 N+1을 피한다.
     */
    @Transactional(readOnly = true)
    public List<CommentListItemResult> execute(UUID postId, UUID requesterId) {
        if (postRepositoryPort.findById(postId).isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }

        List<CommunityCommentListItem> items = communityCommentQueryPort.findByPostId(postId);

        // 댓글 작성자 userId를 한 번에 별칭으로 변환(N+1 회피).
        Set<UUID> authorUserIds =
                items.stream()
                        .map(CommunityCommentListItem::authorUserId)
                        .collect(Collectors.toSet());
        Map<UUID, String> aliasByUser = aliasDisplayResolver.displayNames(authorUserIds);

        // 이 목록의 댓글 중 현재 사용자가 좋아요한 것들을 한 번에 배치 조회(N+1 회피).
        Set<UUID> commentIds =
                items.stream().map(CommunityCommentListItem::id).collect(Collectors.toSet());
        Set<UUID> likedCommentIds =
                commentLikeRepositoryPort.findLikedCommentIds(requesterId, commentIds);

        return items.stream()
                .map(
                        item ->
                                CommentListItemResult.of(
                                        item,
                                        aliasByUser.getOrDefault(
                                                item.authorUserId(),
                                                CommunityAliasDisplayResolver
                                                        .FALLBACK_DISPLAY_NAME),
                                        item.authorUserId().equals(requesterId),
                                        likedCommentIds.contains(item.id())))
                .toList();
    }
}
