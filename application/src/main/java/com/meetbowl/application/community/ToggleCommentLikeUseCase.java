package com.meetbowl.application.community;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.meetbowl.application.notification.DispatchNotificationCommand;
import com.meetbowl.application.notification.DispatchNotificationUseCase;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.Comment;
import com.meetbowl.domain.community.CommentLike;
import com.meetbowl.domain.community.CommentLikeRepositoryPort;
import com.meetbowl.domain.community.CommentRepositoryPort;
import com.meetbowl.domain.notification.NotificationResourceType;
import com.meetbowl.domain.notification.NotificationType;

/**
 * 댓글 좋아요 토글 UseCase다. 게시글 좋아요와 동일한 규칙(한 번만·토글·중복 방지)을 (comment_id, user_id) 유니크 제약으로 보장한다.
 *
 * <p>대상 댓글이 존재하고 경로의 게시글에 속하는지 확인한 뒤 토글한다. 트랜잭션/경합 처리 방식은 {@link TogglePostLikeUseCase}와 같다(검사-실행
 * 사이 동시 좋아요는 유니크 제약으로 한 행만 남기고, insert 충돌을 잡아 '좋아요됨'으로 수렴).
 */
@Service
public class ToggleCommentLikeUseCase {

    private final CommentRepositoryPort commentRepositoryPort;
    private final CommentLikeRepositoryPort commentLikeRepositoryPort;
    private final DispatchNotificationUseCase dispatchNotificationUseCase;

    public ToggleCommentLikeUseCase(
            CommentRepositoryPort commentRepositoryPort,
            CommentLikeRepositoryPort commentLikeRepositoryPort,
            DispatchNotificationUseCase dispatchNotificationUseCase) {
        this.commentRepositoryPort = commentRepositoryPort;
        this.commentLikeRepositoryPort = commentLikeRepositoryPort;
        this.dispatchNotificationUseCase = dispatchNotificationUseCase;
    }

    public LikeToggleResult execute(UUID postId, UUID commentId, UUID userId) {
        Comment comment =
                commentRepositoryPort
                        .findById(commentId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "댓글을 찾을 수 없습니다."));
        // 경로의 게시글과 댓글이 일치하지 않으면 잘못된 참조다.
        if (!comment.postId().equals(postId)) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }

        boolean liked;
        if (commentLikeRepositoryPort.existsByCommentIdAndUserId(commentId, userId)) {
            commentLikeRepositoryPort.deleteByCommentIdAndUserId(commentId, userId);
            liked = false;
        } else {
            try {
                commentLikeRepositoryPort.save(CommentLike.create(commentId, userId));
            } catch (DataIntegrityViolationException concurrentDuplicate) {
                // 같은 사용자의 동시 좋아요로 유니크 제약 충돌 → 한 행만 남는다. 결과는 '좋아요됨' 상태.
            }
            liked = true;
        }

        if (liked && !comment.authorUserId().equals(userId)) {
            dispatchNotificationUseCase.execute(
                    new DispatchNotificationCommand(
                            comment.authorUserId(),
                            NotificationType.COMMUNITY_COMMENT_LIKED.name(),
                            "내 댓글에 좋아요가 추가되었습니다.",
                            comment.content(),
                            NotificationResourceType.COMMUNITY_POST.name(),
                            comment.postId()));
        }

        long likeCount = commentLikeRepositoryPort.countByCommentId(commentId);
        return new LikeToggleResult(liked, likeCount);
    }
}
