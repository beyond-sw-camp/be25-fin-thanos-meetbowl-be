package com.meetbowl.application.community;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.Comment;
import com.meetbowl.domain.community.CommentLikeRepositoryPort;
import com.meetbowl.domain.community.CommentRepositoryPort;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.PostLikeRepositoryPort;
import com.meetbowl.domain.community.PostRepositoryPort;

/**
 * 게시글 삭제 UseCase다. 작성자 본인만 삭제할 수 있다.
 *
 * <p>하드 삭제 + cascade: 게시글이 사라지면 그 글에 달린 댓글·좋아요는 참조 대상이 없어져 의미 없는 고아 행이 된다. 따라서 자식부터 차례로 제거한다 — (1)
 * 각 댓글의 좋아요 → (2) 댓글 → (3) 게시글 좋아요 → (4) 게시글. 같은 트랜잭션에서 처리하므로 일부만 지워지는 상태가 남지 않는다.
 *
 * <p>댓글 좋아요는 게시글 단위 일괄 삭제 포트가 없어, 해당 게시글의 댓글 id를 돌며 댓글별로 제거한다(댓글/좋아요 등록 기능이 추가돼도 안전하도록 미리 cascade).
 */
@Service
public class DeletePostUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final CommentRepositoryPort commentRepositoryPort;
    private final PostLikeRepositoryPort postLikeRepositoryPort;
    private final CommentLikeRepositoryPort commentLikeRepositoryPort;

    public DeletePostUseCase(
            PostRepositoryPort postRepositoryPort,
            CommentRepositoryPort commentRepositoryPort,
            PostLikeRepositoryPort postLikeRepositoryPort,
            CommentLikeRepositoryPort commentLikeRepositoryPort) {
        this.postRepositoryPort = postRepositoryPort;
        this.commentRepositoryPort = commentRepositoryPort;
        this.postLikeRepositoryPort = postLikeRepositoryPort;
        this.commentLikeRepositoryPort = commentLikeRepositoryPort;
    }

    @Transactional
    public void execute(UUID postId, UUID requesterId) {
        Post post =
                postRepositoryPort
                        .findById(postId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "게시글을 찾을 수 없습니다."));

        if (!post.isAuthoredBy(requesterId)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "작성자만 게시글을 삭제할 수 있습니다.");
        }

        // 1. 댓글에 달린 좋아요부터 제거(댓글 단위 일괄 삭제 포트만 있어 댓글 id로 순회).
        for (Comment comment : commentRepositoryPort.findByPostId(postId)) {
            commentLikeRepositoryPort.deleteByCommentId(comment.id());
        }
        // 2. 댓글, 3. 게시글 좋아요, 4. 게시글 순으로 제거.
        commentRepositoryPort.deleteByPostId(postId);
        postLikeRepositoryPort.deleteByPostId(postId);
        postRepositoryPort.deleteById(postId);
    }
}
