package com.meetbowl.application.community;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.application.notification.DispatchNotificationCommand;
import com.meetbowl.application.notification.DispatchNotificationUseCase;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.Comment;
import com.meetbowl.domain.community.CommunityCommentListItem;
import com.meetbowl.domain.community.CommunityCommentQueryPort;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.CommentRepositoryPort;
import com.meetbowl.domain.community.PostRepositoryPort;
import com.meetbowl.domain.notification.NotificationResourceType;
import com.meetbowl.domain.notification.NotificationType;

/**
 * 댓글 등록 UseCase다.
 *
 * <p>흐름: (1) 대상 게시글이 존재하는지 확인한다(없거나 삭제됐으면 404로 거부). (2) 댓글을 저장한다(내용 필수 검증은 도메인 {@link Comment#create}
 * 에서 수행). (3) 저장 후 해당 게시글의 현재 댓글 순서를 기준으로 작성자 표기를 계산한다.
 *
 * <p>게시글의 댓글 수(commentCount)는 별도 카운터 컬럼이 아니라 댓글 행 수를 그때그때 집계해 산출하므로(목록/상세의 COUNT 쿼리), 댓글을 저장하면 카운트
 * 정합성이 자동으로 유지된다 — 따로 증가시킬 값이 없다.
 *
 * <p>익명성: 게시글 작성자는 "글쓴이", 나머지 댓글 작성자는 게시글별 첫 등장 순서대로 "익명1", "익명2"처럼 계산한다.
 */
@Service
public class CreateCommentUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final CommentRepositoryPort commentRepositoryPort;
    private final CommunityCommentQueryPort communityCommentQueryPort;
    private final CommunityAliasPolicy communityAliasPolicy;
    private final DispatchNotificationUseCase dispatchNotificationUseCase;

    public CreateCommentUseCase(
            PostRepositoryPort postRepositoryPort,
            CommentRepositoryPort commentRepositoryPort,
            CommunityCommentQueryPort communityCommentQueryPort,
            CommunityAliasPolicy communityAliasPolicy,
            DispatchNotificationUseCase dispatchNotificationUseCase) {
        this.postRepositoryPort = postRepositoryPort;
        this.commentRepositoryPort = commentRepositoryPort;
        this.communityCommentQueryPort = communityCommentQueryPort;
        this.communityAliasPolicy = communityAliasPolicy;
        this.dispatchNotificationUseCase = dispatchNotificationUseCase;
    }

    @Transactional
    public CommentResult execute(CreateCommentCommand command) {
        Post post =
                postRepositoryPort
                        .findById(command.postId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "게시글을 찾을 수 없습니다."));

        Comment saved =
                commentRepositoryPort.save(
                        Comment.create(
                                command.postId(), command.content(), command.authorUserId()));

        List<CommunityCommentListItem> comments = communityCommentQueryPort.findByPostId(command.postId());
        String authorAlias =
                communityAliasPolicy.commentAuthorAlias(
                        post.authorUserId(), command.authorUserId(), comments);

        notifyPostAuthor(post, command.authorUserId());
        return CommentResult.of(saved, authorAlias);
    }

    private void notifyPostAuthor(Post post, java.util.UUID commenterUserId) {
        if (post.authorUserId().equals(commenterUserId)) {
            return;
        }
        dispatchNotificationUseCase.execute(
                new DispatchNotificationCommand(
                        post.authorUserId(),
                        NotificationType.COMMUNITY_POST_COMMENTED.name(),
                        "내 글에 새 댓글이 달렸습니다.",
                        post.title(),
                        NotificationResourceType.COMMUNITY_POST.name(),
                        post.id()));
    }
}
