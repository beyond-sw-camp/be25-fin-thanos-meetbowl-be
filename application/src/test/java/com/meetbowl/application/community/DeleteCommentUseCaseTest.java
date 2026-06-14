package com.meetbowl.application.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.Comment;
import com.meetbowl.domain.community.CommentLikeRepositoryPort;
import com.meetbowl.domain.community.CommentRepositoryPort;

@ExtendWith(MockitoExtension.class)
class DeleteCommentUseCaseTest {

    private DeleteCommentUseCase deleteCommentUseCase;

    @Mock private CommentRepositoryPort commentRepositoryPort;
    @Mock private CommentLikeRepositoryPort commentLikeRepositoryPort;

    @BeforeEach
    void setUp() {
        deleteCommentUseCase =
                new DeleteCommentUseCase(commentRepositoryPort, commentLikeRepositoryPort);
    }

    @Test
    void deletesCommentWithLikesInCascadeOrder() {
        UUID author = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        given(commentRepositoryPort.findById(commentId))
                .willReturn(Optional.of(Comment.of(commentId, postId, "c", author)));

        deleteCommentUseCase.execute(postId, commentId, author);

        // 자식(댓글 좋아요) → 댓글 순.
        InOrder order = inOrder(commentLikeRepositoryPort, commentRepositoryPort);
        order.verify(commentLikeRepositoryPort).deleteByCommentId(commentId);
        order.verify(commentRepositoryPort).deleteById(commentId);
    }

    @Test
    void rejectsWhenRequesterIsNotAuthor() {
        UUID author = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        given(commentRepositoryPort.findById(commentId))
                .willReturn(Optional.of(Comment.of(commentId, postId, "c", author)));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deleteCommentUseCase.execute(postId, commentId, intruder));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
        verify(commentRepositoryPort, never()).deleteById(any());
        verify(commentLikeRepositoryPort, never()).deleteByCommentId(any());
    }

    @Test
    void rejectsWhenCommentBelongsToAnotherPost() {
        UUID author = UUID.randomUUID();
        UUID requestedPostId = UUID.randomUUID();
        UUID actualPostId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        given(commentRepositoryPort.findById(commentId))
                .willReturn(Optional.of(Comment.of(commentId, actualPostId, "c", author)));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deleteCommentUseCase.execute(requestedPostId, commentId, author));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
        verify(commentRepositoryPort, never()).deleteById(any());
    }

    @Test
    void throwsNotFoundWhenCommentMissing() {
        UUID commentId = UUID.randomUUID();
        given(commentRepositoryPort.findById(commentId)).willReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                deleteCommentUseCase.execute(
                                        UUID.randomUUID(), commentId, UUID.randomUUID()));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
    }
}
