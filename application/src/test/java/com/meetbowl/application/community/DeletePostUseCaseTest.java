package com.meetbowl.application.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
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
import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.PostLikeRepositoryPort;
import com.meetbowl.domain.community.PostRepositoryPort;

@ExtendWith(MockitoExtension.class)
class DeletePostUseCaseTest {

    private DeletePostUseCase deletePostUseCase;

    @Mock private PostRepositoryPort postRepositoryPort;
    @Mock private CommentRepositoryPort commentRepositoryPort;
    @Mock private PostLikeRepositoryPort postLikeRepositoryPort;
    @Mock private CommentLikeRepositoryPort commentLikeRepositoryPort;

    @BeforeEach
    void setUp() {
        deletePostUseCase =
                new DeletePostUseCase(
                        postRepositoryPort,
                        commentRepositoryPort,
                        postLikeRepositoryPort,
                        commentLikeRepositoryPort);
    }

    @Test
    void deletesPostWithChildrenInCascadeOrder() {
        UUID author = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        given(postRepositoryPort.findById(postId))
                .willReturn(
                        Optional.of(Post.of(postId, CommunityCategory.FREE, "t", "c", author, 0L)));
        given(commentRepositoryPort.findByPostId(postId))
                .willReturn(List.of(Comment.of(commentId, postId, "댓글", UUID.randomUUID())));

        deletePostUseCase.execute(postId, author);

        // 자식부터 부모 순서: 댓글 좋아요 → 댓글 → 게시글 좋아요 → 게시글.
        InOrder order =
                inOrder(
                        commentLikeRepositoryPort,
                        commentRepositoryPort,
                        postLikeRepositoryPort,
                        postRepositoryPort);
        order.verify(commentLikeRepositoryPort).deleteByCommentId(commentId);
        order.verify(commentRepositoryPort).deleteByPostId(postId);
        order.verify(postLikeRepositoryPort).deleteByPostId(postId);
        order.verify(postRepositoryPort).deleteById(postId);
    }

    @Test
    void rejectsWhenRequesterIsNotAuthor() {
        UUID author = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        given(postRepositoryPort.findById(postId))
                .willReturn(
                        Optional.of(Post.of(postId, CommunityCategory.FREE, "t", "c", author, 0L)));

        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> deletePostUseCase.execute(postId, intruder));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
        // 권한 실패 시 어떤 것도 삭제하지 않는다.
        verify(postRepositoryPort, never()).deleteById(any());
        verify(commentRepositoryPort, never()).deleteByPostId(any());
        verify(postLikeRepositoryPort, never()).deleteByPostId(any());
    }

    @Test
    void throwsNotFoundWhenPostMissing() {
        UUID postId = UUID.randomUUID();
        given(postRepositoryPort.findById(postId)).willReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> deletePostUseCase.execute(postId, UUID.randomUUID()));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
    }
}
