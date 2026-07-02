package com.meetbowl.application.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.CommentLikeRepositoryPort;
import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.CommunityCommentListItem;
import com.meetbowl.domain.community.CommunityCommentQueryPort;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.PostRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ListCommentsUseCaseTest {

    private ListCommentsUseCase listCommentsUseCase;

    @Mock private PostRepositoryPort postRepositoryPort;
    @Mock private CommunityCommentQueryPort communityCommentQueryPort;
    @Mock private CommentLikeRepositoryPort commentLikeRepositoryPort;

    @BeforeEach
    void setUp() {
        listCommentsUseCase =
                new ListCommentsUseCase(
                        postRepositoryPort,
                        communityCommentQueryPort,
                        new CommunityAliasPolicy(),
                        commentLikeRepositoryPort);
    }

    @Test
    void mapsAuthorsToAliasDisplayNamesWithMineAndLiked() {
        UUID postId = UUID.randomUUID();
        UUID requester = UUID.randomUUID();
        UUID author2 = UUID.randomUUID();
        UUID comment1Id = UUID.randomUUID();
        UUID comment2Id = UUID.randomUUID();
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
        // 첫 댓글은 요청자 본인 작성, 둘째는 타인 작성.
        given(communityCommentQueryPort.findByPostId(postId))
                .willReturn(
                        List.of(
                                new CommunityCommentListItem(
                                        comment1Id, requester, "댓글1", 2L, Instant.now()),
                                new CommunityCommentListItem(
                                        comment2Id, author2, "댓글2", 0L, Instant.now())));
        // 요청자는 둘째 댓글에만 좋아요를 눌렀다.
        given(commentLikeRepositoryPort.findLikedCommentIds(eq(requester), anyCollection()))
                .willReturn(Set.of(comment2Id));

        List<CommentListItemResult> results = listCommentsUseCase.execute(postId, requester);

        assertEquals(2, results.size());
        assertEquals("익명1", results.get(0).authorAlias());
        assertEquals("댓글1", results.get(0).content());
        assertEquals(2L, results.get(0).likeCount());
        assertTrue(results.get(0).mine());
        assertFalse(results.get(0).liked());
        assertEquals("익명2", results.get(1).authorAlias());
        assertFalse(results.get(1).mine());
        assertTrue(results.get(1).liked());
    }

    @Test
    void rejectsWhenPostMissing() {
        UUID postId = UUID.randomUUID();
        given(postRepositoryPort.findById(postId)).willReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> listCommentsUseCase.execute(postId, UUID.randomUUID()));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
        verify(communityCommentQueryPort, never()).findByPostId(postId);
    }
}
