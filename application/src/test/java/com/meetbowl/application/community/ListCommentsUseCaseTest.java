package com.meetbowl.application.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
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
    @Mock private CommunityAliasDisplayResolver aliasDisplayResolver;

    @BeforeEach
    void setUp() {
        listCommentsUseCase =
                new ListCommentsUseCase(
                        postRepositoryPort, communityCommentQueryPort, aliasDisplayResolver);
    }

    @Test
    void mapsAuthorsToAliasDisplayNames() {
        UUID postId = UUID.randomUUID();
        UUID author1 = UUID.randomUUID();
        UUID author2 = UUID.randomUUID();
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
        given(communityCommentQueryPort.findByPostId(postId))
                .willReturn(
                        List.of(
                                new CommunityCommentListItem(
                                        UUID.randomUUID(), author1, "댓글1", 2L, Instant.now()),
                                new CommunityCommentListItem(
                                        UUID.randomUUID(), author2, "댓글2", 0L, Instant.now())));
        given(aliasDisplayResolver.displayNames(anyCollection()))
                .willReturn(Map.of(author1, "익명1", author2, "익명2"));

        List<CommentListItemResult> results = listCommentsUseCase.execute(postId);

        assertEquals(2, results.size());
        assertEquals("익명1", results.get(0).authorAlias());
        assertEquals("댓글1", results.get(0).content());
        assertEquals(2L, results.get(0).likeCount());
        assertEquals("익명2", results.get(1).authorAlias());
    }

    @Test
    void rejectsWhenPostMissing() {
        UUID postId = UUID.randomUUID();
        given(postRepositoryPort.findById(postId)).willReturn(Optional.empty());

        BusinessException exception =
                assertThrows(BusinessException.class, () -> listCommentsUseCase.execute(postId));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
        verify(communityCommentQueryPort, never()).findByPostId(postId);
    }
}
