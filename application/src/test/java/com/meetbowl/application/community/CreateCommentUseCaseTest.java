package com.meetbowl.application.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetbowl.application.notification.DispatchNotificationUseCase;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.Comment;
import com.meetbowl.domain.community.CommentRepositoryPort;
import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.CommunityCommentListItem;
import com.meetbowl.domain.community.CommunityCommentQueryPort;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.PostRepositoryPort;

@ExtendWith(MockitoExtension.class)
class CreateCommentUseCaseTest {

    private CreateCommentUseCase createCommentUseCase;

    @Mock private PostRepositoryPort postRepositoryPort;
    @Mock private CommentRepositoryPort commentRepositoryPort;
    @Mock private CommunityCommentQueryPort communityCommentQueryPort;
    @Mock private DispatchNotificationUseCase dispatchNotificationUseCase;

    @BeforeEach
    void setUp() {
        createCommentUseCase =
                new CreateCommentUseCase(
                        postRepositoryPort,
                        commentRepositoryPort,
                        communityCommentQueryPort,
                        new CommunityAliasPolicy(),
                        dispatchNotificationUseCase);
    }

    @Test
    void createsCommentAssigningPerPostAlias() {
        UUID postAuthor = UUID.randomUUID();
        UUID commenter = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        given(postRepositoryPort.findById(postId))
                .willReturn(
                        Optional.of(
                                Post.of(postId, CommunityCategory.FREE, "t", "c", postAuthor, 0L)));
        given(commentRepositoryPort.save(any(Comment.class))).willAnswer(inv -> inv.getArgument(0));
        given(communityCommentQueryPort.findByPostId(postId))
                .willReturn(
                        List.of(
                                new CommunityCommentListItem(
                                        UUID.randomUUID(),
                                        commenter,
                                        "댓글 내용",
                                        0L,
                                        Instant.now())));

        CommentResult result =
                createCommentUseCase.execute(new CreateCommentCommand(postId, "댓글 내용", commenter));

        assertEquals("익명1", result.authorAlias());
        assertEquals("댓글 내용", result.content());
        assertEquals(postId, result.postId());
        assertEquals(0L, result.likeCount());
        verify(communityCommentQueryPort).findByPostId(postId);
    }

    @Test
    void rejectsWhenPostMissing() {
        UUID postId = UUID.randomUUID();
        given(postRepositoryPort.findById(postId)).willReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                createCommentUseCase.execute(
                                        new CreateCommentCommand(postId, "내용", UUID.randomUUID())));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
        verify(commentRepositoryPort, never()).save(any());
        verify(communityCommentQueryPort, never()).findByPostId(any());
    }
}
