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
import com.meetbowl.domain.community.Comment;
import com.meetbowl.domain.community.CommentRepositoryPort;
import com.meetbowl.domain.community.CommunityAlias;
import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.PostRepositoryPort;

@ExtendWith(MockitoExtension.class)
class CreateCommentUseCaseTest {

    private CreateCommentUseCase createCommentUseCase;

    @Mock private PostRepositoryPort postRepositoryPort;
    @Mock private CommentRepositoryPort commentRepositoryPort;
    @Mock private CommunityAliasResolver communityAliasResolver;

    @BeforeEach
    void setUp() {
        createCommentUseCase =
                new CreateCommentUseCase(
                        postRepositoryPort, commentRepositoryPort, communityAliasResolver);
    }

    @Test
    void createsCommentReusingExistingAlias() {
        UUID author = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        given(postRepositoryPort.findById(postId))
                .willReturn(
                        Optional.of(Post.of(postId, CommunityCategory.FREE, "t", "c", author, 0L)));
        // 게시글에서 이미 익명1을 받은 사용자 → 댓글에서도 같은 별칭을 재사용한다.
        given(communityAliasResolver.resolve(author))
                .willReturn(CommunityAlias.of(UUID.randomUUID(), author, 1));
        given(commentRepositoryPort.save(any(Comment.class))).willAnswer(inv -> inv.getArgument(0));

        CommentResult result =
                createCommentUseCase.execute(new CreateCommentCommand(postId, "댓글 내용", author));

        assertEquals("익명1", result.authorAlias());
        assertEquals("댓글 내용", result.content());
        assertEquals(postId, result.postId());
        assertEquals(0L, result.likeCount());
        // 별칭 채번은 기존 서비스에 위임하고 새로 만들지 않는다.
        verify(communityAliasResolver).resolve(author);
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
        verify(communityAliasResolver, never()).resolve(any());
    }
}
