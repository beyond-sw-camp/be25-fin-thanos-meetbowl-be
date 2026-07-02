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
import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.PostLike;
import com.meetbowl.domain.community.PostLikeRepositoryPort;
import com.meetbowl.domain.community.PostRepositoryPort;

@ExtendWith(MockitoExtension.class)
class TogglePostLikeUseCaseTest {

    private TogglePostLikeUseCase togglePostLikeUseCase;

    @Mock private PostRepositoryPort postRepositoryPort;
    @Mock private PostLikeRepositoryPort postLikeRepositoryPort;
    @Mock private DispatchNotificationUseCase dispatchNotificationUseCase;

    @BeforeEach
    void setUp() {
        togglePostLikeUseCase =
                new TogglePostLikeUseCase(
                        postRepositoryPort, postLikeRepositoryPort, dispatchNotificationUseCase);
    }

    private void givenPostExists(UUID postId) {
        given(postRepositoryPort.findById(postId))
                .willReturn(
                        Optional.of(
                                Post.of(
                                        postId,
                                        CommunityCategory.FREE,
                                        "t",
                                        "c",
                                        UUID.randomUUID(),
                                        0L)));
    }

    @Test
    void addsLikeWhenNotLikedYet() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        givenPostExists(postId);
        given(postLikeRepositoryPort.existsByPostIdAndUserId(postId, userId)).willReturn(false);
        given(postLikeRepositoryPort.countByPostId(postId)).willReturn(1L);

        LikeToggleResult result = togglePostLikeUseCase.execute(postId, userId);

        assertTrue(result.liked());
        assertEquals(1L, result.likeCount());
        verify(postLikeRepositoryPort).save(any(PostLike.class));
        verify(postLikeRepositoryPort, never()).deleteByPostIdAndUserId(any(), any());
    }

    @Test
    void cancelsLikeWhenAlreadyLiked() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        givenPostExists(postId);
        given(postLikeRepositoryPort.existsByPostIdAndUserId(postId, userId)).willReturn(true);
        given(postLikeRepositoryPort.countByPostId(postId)).willReturn(0L);

        LikeToggleResult result = togglePostLikeUseCase.execute(postId, userId);

        assertFalse(result.liked());
        assertEquals(0L, result.likeCount());
        verify(postLikeRepositoryPort).deleteByPostIdAndUserId(postId, userId);
        verify(postLikeRepositoryPort, never()).save(any());
    }

    @Test
    void treatsConcurrentDuplicateInsertAsLiked() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        givenPostExists(postId);
        given(postLikeRepositoryPort.existsByPostIdAndUserId(postId, userId)).willReturn(false);
        // 같은 사용자가 동시에 두 번 눌러 유니크 제약 충돌이 난 상황을 흉내낸다.
        willThrow(new DataIntegrityViolationException("duplicate"))
                .given(postLikeRepositoryPort)
                .save(any(PostLike.class));
        given(postLikeRepositoryPort.countByPostId(postId)).willReturn(1L);

        LikeToggleResult result = togglePostLikeUseCase.execute(postId, userId);

        // 충돌을 잡아 '좋아요됨' 상태로 수렴하고, 중복 행 없이 카운트는 1.
        assertTrue(result.liked());
        assertEquals(1L, result.likeCount());
    }

    @Test
    void rejectsWhenPostMissing() {
        UUID postId = UUID.randomUUID();
        given(postRepositoryPort.findById(postId)).willReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> togglePostLikeUseCase.execute(postId, UUID.randomUUID()));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
        verify(postLikeRepositoryPort, never()).save(any());
        verify(postLikeRepositoryPort, never()).deleteByPostIdAndUserId(any(), any());
    }
}
