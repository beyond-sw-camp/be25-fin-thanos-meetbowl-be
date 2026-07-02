package com.meetbowl.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.CommunityPostListItem;
import com.meetbowl.domain.community.CommunityPostQueryPort;
import com.meetbowl.domain.community.PostLikeRepositoryPort;

@ExtendWith(MockitoExtension.class)
class GetHotPostsUseCaseTest {

    @Mock private CommunityPostQueryPort communityPostQueryPort;
    @Mock private PostLikeRepositoryPort postLikeRepositoryPort;

    private GetHotPostsUseCase getHotPostsUseCase;

    @BeforeEach
    void setUp() {
        getHotPostsUseCase =
                new GetHotPostsUseCase(
                        communityPostQueryPort, new CommunityAliasPolicy(), postLikeRepositoryPort);
    }

    private CommunityPostListItem item(UUID id, UUID author) {
        return new CommunityPostListItem(
                id, CommunityCategory.FREE, "t", "c", author, 0L, 0L, 0L, Instant.now());
    }

    @Test
    void anonymousRequesterSkipsLikeLookupAndReturnsMineLikedFalse() {
        UUID author = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        given(communityPostQueryPort.findHot(any(), anyInt()))
                .willReturn(List.of(item(postId, author)));

        // 비로그인(requesterId=null)도 NPE 없이 조회되고, mine·liked는 모두 false다.
        List<PostListItemResult> results = getHotPostsUseCase.execute(null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).mine()).isFalse();
        assertThat(results.get(0).liked()).isFalse();
        // 비로그인이면 좋아요 배치 조회 자체를 생략한다.
        verify(postLikeRepositoryPort, never()).findLikedPostIds(any(), any());
    }

    @Test
    void loggedInRequesterComputesMineAndLiked() {
        UUID requester = UUID.randomUUID();
        UUID otherAuthor = UUID.randomUUID();
        UUID minePostId = UUID.randomUUID();
        UUID likedOtherPostId = UUID.randomUUID();
        given(communityPostQueryPort.findHot(any(), anyInt()))
                .willReturn(
                        List.of(item(minePostId, requester), item(likedOtherPostId, otherAuthor)));
        given(postLikeRepositoryPort.findLikedPostIds(eq(requester), any()))
                .willReturn(Set.of(likedOtherPostId));

        List<PostListItemResult> results = getHotPostsUseCase.execute(requester);

        PostListItemResult mine =
                results.stream().filter(r -> r.id().equals(minePostId)).findFirst().orElseThrow();
        PostListItemResult likedOther =
                results.stream()
                        .filter(r -> r.id().equals(likedOtherPostId))
                        .findFirst()
                        .orElseThrow();
        assertThat(mine.mine()).isTrue();
        assertThat(mine.liked()).isFalse();
        assertThat(likedOther.mine()).isFalse();
        assertThat(likedOther.liked()).isTrue();
    }
}
