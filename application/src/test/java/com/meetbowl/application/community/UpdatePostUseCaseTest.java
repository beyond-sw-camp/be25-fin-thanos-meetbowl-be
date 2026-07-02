package com.meetbowl.application.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.CommentRepositoryPort;
import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.PostLikeRepositoryPort;
import com.meetbowl.domain.community.PostRepositoryPort;

@ExtendWith(MockitoExtension.class)
class UpdatePostUseCaseTest {

    private UpdatePostUseCase updatePostUseCase;

    @Mock private PostRepositoryPort postRepositoryPort;
    @Mock private PostLikeRepositoryPort postLikeRepositoryPort;
    @Mock private CommentRepositoryPort commentRepositoryPort;
    @BeforeEach
    void setUp() {
        updatePostUseCase =
                new UpdatePostUseCase(
                        postRepositoryPort,
                        postLikeRepositoryPort,
                        commentRepositoryPort,
                        new CommunityAliasPolicy());
    }

    @Test
    void updatesWhenRequesterIsAuthor() {
        UUID author = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post existing = Post.of(postId, CommunityCategory.FREE, "old", "old body", author, 7L);
        given(postRepositoryPort.findById(postId)).willReturn(Optional.of(existing));
        given(postRepositoryPort.save(any(Post.class))).willAnswer(inv -> inv.getArgument(0));
        given(postLikeRepositoryPort.countByPostId(postId)).willReturn(4L);
        given(commentRepositoryPort.countByPostId(postId)).willReturn(2L);
        PostResult result =
                updatePostUseCase.execute(
                        new UpdatePostCommand(postId, author, "맛집", "새 제목", "새 내용"));

        // 카테고리·제목·내용은 바뀌고, 조회수/작성자는 보존, 좋아요/댓글 수는 현재 값을 읽어 응답.
        assertEquals("RESTAURANT", result.category());
        assertEquals("새 제목", result.title());
        assertEquals("새 내용", result.content());
        assertEquals(7L, result.viewCount());
        assertEquals(4L, result.likeCount());
        assertEquals(2L, result.commentCount());
        assertEquals(CommunityAliasPolicy.POST_AUTHOR_ALIAS, result.authorAlias());
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
                        BusinessException.class,
                        () ->
                                updatePostUseCase.execute(
                                        new UpdatePostCommand(postId, intruder, "자유", "t2", "c2")));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
        verify(postRepositoryPort, never()).save(any());
    }

    @Test
    void throwsNotFoundWhenPostMissing() {
        UUID postId = UUID.randomUUID();
        given(postRepositoryPort.findById(postId)).willReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                updatePostUseCase.execute(
                                        new UpdatePostCommand(
                                                postId, UUID.randomUUID(), "자유", "t", "c")));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
    }
}
