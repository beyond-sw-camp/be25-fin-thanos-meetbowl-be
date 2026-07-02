package com.meetbowl.application.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.meetbowl.application.notification.DispatchNotificationUseCase;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.Comment;
import com.meetbowl.domain.community.CommentLike;
import com.meetbowl.domain.community.CommentLikeRepositoryPort;
import com.meetbowl.domain.community.CommentRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ToggleCommentLikeUseCaseTest {

    private ToggleCommentLikeUseCase toggleCommentLikeUseCase;

    @Mock private CommentRepositoryPort commentRepositoryPort;
    @Mock private CommentLikeRepositoryPort commentLikeRepositoryPort;
    @Mock private DispatchNotificationUseCase dispatchNotificationUseCase;

    @BeforeEach
    void setUp() {
        toggleCommentLikeUseCase =
                new ToggleCommentLikeUseCase(
                        commentRepositoryPort,
                        commentLikeRepositoryPort,
                        dispatchNotificationUseCase);
    }

    @Test
    void addsLikeWhenNotLikedYet() {
        UUID postId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(commentRepositoryPort.findById(commentId))
                .willReturn(Optional.of(Comment.of(commentId, postId, "댓글", UUID.randomUUID())));
        given(commentLikeRepositoryPort.existsByCommentIdAndUserId(commentId, userId))
                .willReturn(false);
        given(commentLikeRepositoryPort.countByCommentId(commentId)).willReturn(1L);

        LikeToggleResult result = toggleCommentLikeUseCase.execute(postId, commentId, userId);

        assertTrue(result.liked());
        assertEquals(1L, result.likeCount());
        verify(commentLikeRepositoryPort).save(any(CommentLike.class));
    }

    @Test
    void cancelsLikeWhenAlreadyLiked() {
        UUID postId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(commentRepositoryPort.findById(commentId))
                .willReturn(Optional.of(Comment.of(commentId, postId, "댓글", UUID.randomUUID())));
        given(commentLikeRepositoryPort.existsByCommentIdAndUserId(commentId, userId))
                .willReturn(true);
        given(commentLikeRepositoryPort.countByCommentId(commentId)).willReturn(0L);

        LikeToggleResult result = toggleCommentLikeUseCase.execute(postId, commentId, userId);

        assertFalse(result.liked());
        assertEquals(0L, result.likeCount());
        verify(commentLikeRepositoryPort).deleteByCommentIdAndUserId(commentId, userId);
        verify(commentLikeRepositoryPort, never()).save(any());
    }

    @Test
    void treatsConcurrentDuplicateInsertAsLiked() {
        UUID postId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(commentRepositoryPort.findById(commentId))
                .willReturn(Optional.of(Comment.of(commentId, postId, "댓글", UUID.randomUUID())));
        given(commentLikeRepositoryPort.existsByCommentIdAndUserId(commentId, userId))
                .willReturn(false);
        willThrow(new DataIntegrityViolationException("duplicate"))
                .given(commentLikeRepositoryPort)
                .save(any(CommentLike.class));
        given(commentLikeRepositoryPort.countByCommentId(commentId)).willReturn(1L);

        LikeToggleResult result = toggleCommentLikeUseCase.execute(postId, commentId, userId);

        assertTrue(result.liked());
        assertEquals(1L, result.likeCount());
    }

    @Test
    void rejectsWhenCommentMissing() {
        UUID commentId = UUID.randomUUID();
        given(commentRepositoryPort.findById(commentId)).willReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                toggleCommentLikeUseCase.execute(
                                        UUID.randomUUID(), commentId, UUID.randomUUID()));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
    }

    @Test
    void rejectsWhenCommentBelongsToAnotherPost() {
        UUID commentId = UUID.randomUUID();
        UUID requestedPostId = UUID.randomUUID();
        UUID actualPostId = UUID.randomUUID();
        // 댓글은 다른 게시글에 속함 → 경로 불일치로 404.
        given(commentRepositoryPort.findById(commentId))
                .willReturn(
                        Optional.of(Comment.of(commentId, actualPostId, "댓글", UUID.randomUUID())));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                toggleCommentLikeUseCase.execute(
                                        requestedPostId, commentId, UUID.randomUUID()));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
        verify(commentLikeRepositoryPort, never()).save(any());
    }
}
