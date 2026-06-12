package com.meetbowl.domain.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class CommentTest {

    @Test
    void changeUpdatesContentAndPreservesRest() {
        UUID postId = UUID.randomUUID();
        UUID author = UUID.randomUUID();
        Comment comment = Comment.create(postId, "원본 댓글", author);

        Comment changed = comment.change("수정 댓글");

        assertEquals("수정 댓글", changed.content());
        assertEquals(postId, changed.postId());
        assertEquals(author, changed.authorUserId());
    }

    @Test
    void isAuthoredByChecksOwner() {
        UUID author = UUID.randomUUID();
        Comment comment = Comment.create(UUID.randomUUID(), "댓글", author);

        assertTrue(comment.isAuthoredBy(author));
        assertFalse(comment.isAuthoredBy(UUID.randomUUID()));
    }

    @Test
    void rejectsBlankContent() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> Comment.create(UUID.randomUUID(), " ", UUID.randomUUID()));
        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void rejectsNullPostId() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> Comment.create(null, "댓글", UUID.randomUUID()));
        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void rejectsNullAuthor() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> Comment.create(UUID.randomUUID(), "댓글", null));
        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }
}
