package com.meetbowl.application.community;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.Comment;
import com.meetbowl.domain.community.CommentLikeRepositoryPort;
import com.meetbowl.domain.community.CommentRepositoryPort;

/**
 * 댓글 삭제 UseCase다. 작성자 본인만 삭제할 수 있다.
 *
 * <p>대상 댓글이 존재하고 경로의 게시글에 속하는지 확인한 뒤(아니면 404), 작성자 본인인지 검증한다(아니면 403). 하드 삭제 + cascade: 댓글이 사라지면 그
 * 댓글의 좋아요는 의미 없는 고아 행이 되므로 (1) 댓글 좋아요 → (2) 댓글 순으로 같은 트랜잭션에서 제거한다. 게시글의 댓글 수(commentCount)는 행 수 집계라
 * 댓글이 사라지면 자동으로 줄어든다.
 */
@Service
public class DeleteCommentUseCase {

    private final CommentRepositoryPort commentRepositoryPort;
    private final CommentLikeRepositoryPort commentLikeRepositoryPort;

    public DeleteCommentUseCase(
            CommentRepositoryPort commentRepositoryPort,
            CommentLikeRepositoryPort commentLikeRepositoryPort) {
        this.commentRepositoryPort = commentRepositoryPort;
        this.commentLikeRepositoryPort = commentLikeRepositoryPort;
    }

    @Transactional
    public void execute(UUID postId, UUID commentId, UUID requesterId) {
        Comment comment =
                commentRepositoryPort
                        .findById(commentId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "댓글을 찾을 수 없습니다."));
        if (!comment.postId().equals(postId)) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }
        if (!comment.isAuthoredBy(requesterId)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "작성자만 댓글을 삭제할 수 있습니다.");
        }

        // 자식(댓글 좋아요)부터 제거한 뒤 댓글을 제거한다.
        commentLikeRepositoryPort.deleteByCommentId(commentId);
        commentRepositoryPort.deleteById(commentId);
    }
}
