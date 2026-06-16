package com.meetbowl.application.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.Comment;
import com.meetbowl.domain.community.CommentLikeRepositoryPort;
import com.meetbowl.domain.community.CommentRepositoryPort;

@ExtendWith(MockitoExtension.class)
class UpdateCommentUseCaseTest {

    private UpdateCommentUseCase updateCommentUseCase;

    @Mock private CommentRepositoryPort commentRepositoryPort;
    @Mock private CommentLikeRepositoryPort commentLikeRepositoryPort;
    @Mock private CommunityAliasDisplayResolver aliasDisplayResolver;

    @BeforeEach
    void setUp() {
        updateCommentUseCase =
                new UpdateCommentUseCase(
                        commentRepositoryPort, commentLikeRepositoryPort, aliasDisplayResolver);
    }

    @Test
    void updatesContentWhenRequesterIsAuthor() {
        UUID author = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        given(commentRepositoryPort.findById(commentId))
                .willReturn(Optional.of(Comment.of(commentId, postId, "old", author)));
        given(commentRepositoryPort.save(any(Comment.class))).willAnswer(inv -> inv.getArgument(0));
        given(commentLikeRepositoryPort.countByCommentId(commentId)).willReturn(3L);
        given(aliasDisplayResolver.displayNames(anyCollection())).willReturn(Map.of(author, "익명2"));

        CommentResult result =
                updateCommentUseCase.execute(
                        new UpdateCommentCommand(postId, commentId, author, "고친 내용"));

        assertEquals("고친 내용", result.content());
        assertEquals("익명2", result.authorAlias());
        // 좋아요 수는 수정 대상이 아니라 현재 값을 읽어 응답.
        assertEquals(3L, result.likeCount());
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
                        () ->
                                updateCommentUseCase.execute(
                                        new UpdateCommentCommand(
                                                postId, commentId, intruder, "x")));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
        verify(commentRepositoryPort, never()).save(any());
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
                        () ->
                                updateCommentUseCase.execute(
                                        new UpdateCommentCommand(
                                                requestedPostId, commentId, author, "x")));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
        verify(commentRepositoryPort, never()).save(any());
    }

    @Test
    void throwsNotFoundWhenCommentMissing() {
        UUID commentId = UUID.randomUUID();
        given(commentRepositoryPort.findById(commentId)).willReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                updateCommentUseCase.execute(
                                        new UpdateCommentCommand(
                                                UUID.randomUUID(),
                                                commentId,
                                                UUID.randomUUID(),
                                                "x")));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
    }
}
