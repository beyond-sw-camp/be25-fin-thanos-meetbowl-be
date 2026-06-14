package com.meetbowl.application.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.CommunityPostListItem;
import com.meetbowl.domain.community.CommunityPostQueryPort;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.PostLikeRepositoryPort;
import com.meetbowl.domain.community.PostRepositoryPort;

@ExtendWith(MockitoExtension.class)
class GetPostDetailUseCaseTest {

    private GetPostDetailUseCase getPostDetailUseCase;

    @Mock private PostRepositoryPort postRepositoryPort;
    @Mock private CommunityPostQueryPort communityPostQueryPort;
    @Mock private PostLikeRepositoryPort postLikeRepositoryPort;
    @Mock private CommunityAliasDisplayResolver aliasDisplayResolver;

    @Captor private ArgumentCaptor<Post> savedPostCaptor;

    @BeforeEach
    void setUp() {
        getPostDetailUseCase =
                new GetPostDetailUseCase(
                        postRepositoryPort,
                        communityPostQueryPort,
                        postLikeRepositoryPort,
                        aliasDisplayResolver);
    }

    @Test
    void incrementsViewCountAndReturnsDetailWithAliasAndLiked() {
        UUID author = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        // 현재 조회수 5.
        given(postRepositoryPort.findById(postId))
                .willReturn(
                        Optional.of(
                                Post.of(postId, CommunityCategory.FREE, "제목", "본문", author, 5L)));
        // 조회 전용 단건은 증가된 6과 카운트·작성 시각을 반영해 돌려준다고 가정.
        given(communityPostQueryPort.findById(postId))
                .willReturn(
                        Optional.of(
                                new CommunityPostListItem(
                                        postId,
                                        CommunityCategory.FREE,
                                        "제목",
                                        "본문",
                                        author,
                                        6L,
                                        2L,
                                        3L,
                                        Instant.now())));
        given(aliasDisplayResolver.displayNames(anyCollection())).willReturn(Map.of(author, "익명4"));
        given(postLikeRepositoryPort.existsByPostIdAndUserId(postId, viewer)).willReturn(true);

        PostDetailResult result = getPostDetailUseCase.execute(postId, viewer);

        // 저장된 글의 조회수는 1 증가한 6이어야 한다.
        verify(postRepositoryPort).save(savedPostCaptor.capture());
        assertEquals(6L, savedPostCaptor.getValue().viewCount());

        assertEquals(6L, result.viewCount());
        assertEquals(2L, result.likeCount());
        assertEquals(3L, result.commentCount());
        assertEquals("익명4", result.authorAlias());
        assertTrue(result.liked());
    }

    @Test
    void throwsNotFoundWhenPostMissing() {
        UUID postId = UUID.randomUUID();
        given(postRepositoryPort.findById(postId)).willReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> getPostDetailUseCase.execute(postId, UUID.randomUUID()));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
        // 글이 없으면 조회수 증가도 일어나지 않는다.
        verify(postRepositoryPort, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
