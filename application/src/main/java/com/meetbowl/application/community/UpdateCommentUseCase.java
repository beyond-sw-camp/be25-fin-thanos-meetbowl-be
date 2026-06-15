package com.meetbowl.application.community;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.Comment;
import com.meetbowl.domain.community.CommentLikeRepositoryPort;
import com.meetbowl.domain.community.CommentRepositoryPort;

/**
 * 댓글 수정 UseCase다. 작성자 본인만 수정할 수 있고, 수정 항목은 내용뿐이다.
 *
 * <p>대상 댓글이 존재하고 경로의 게시글에 속하는지 확인한 뒤(아니면 404), 작성자 본인인지 검증한다(아니면 403). 좋아요 수는 수정 대상이 아니라 응답 표시용으로
 * 현재 값을 읽기만 한다.
 */
@Service
public class UpdateCommentUseCase {

    private final CommentRepositoryPort commentRepositoryPort;
    private final CommentLikeRepositoryPort commentLikeRepositoryPort;
    private final CommunityAliasDisplayResolver aliasDisplayResolver;

    public UpdateCommentUseCase(
            CommentRepositoryPort commentRepositoryPort,
            CommentLikeRepositoryPort commentLikeRepositoryPort,
            CommunityAliasDisplayResolver aliasDisplayResolver) {
        this.commentRepositoryPort = commentRepositoryPort;
        this.commentLikeRepositoryPort = commentLikeRepositoryPort;
        this.aliasDisplayResolver = aliasDisplayResolver;
    }

    @Transactional
    public CommentResult execute(UpdateCommentCommand command) {
        Comment comment =
                commentRepositoryPort
                        .findById(command.commentId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "댓글을 찾을 수 없습니다."));
        // 경로의 게시글과 댓글이 일치하지 않으면 잘못된 참조다.
        if (!comment.postId().equals(command.postId())) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }
        // 작성자 본인만 수정 가능(비교는 비공개 authorUserId로).
        if (!comment.isAuthoredBy(command.requesterId())) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "작성자만 댓글을 수정할 수 있습니다.");
        }

        // 내용 검증과 작성자 보존은 도메인 change에서 수행된다.
        Comment saved = commentRepositoryPort.save(comment.change(command.content()));

        long likeCount = commentLikeRepositoryPort.countByCommentId(saved.id());
        String authorAlias =
                aliasDisplayResolver
                        .displayNames(Set.of(command.requesterId()))
                        .getOrDefault(
                                command.requesterId(),
                                CommunityAliasDisplayResolver.FALLBACK_DISPLAY_NAME);

        return CommentResult.of(saved, authorAlias, likeCount);
    }
}
