package com.meetbowl.domain.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class PostTest {

    private Post sample(UUID author) {
        return Post.create(CommunityCategory.FREE, "제목", "내용", author);
    }

    @Test
    void createStartsWithZeroView() {
        Post post = sample(UUID.randomUUID());

        assertEquals(0L, post.viewCount());
        assertEquals(CommunityCategory.FREE, post.category());
    }

    @Test
    void changeUpdatesContentAndPreservesAuthorAndView() {
        UUID author = UUID.randomUUID();
        Post post = sample(author).increaseViewCount();

        Post changed = post.change(CommunityCategory.HOBBY, "새 제목", "새 내용");

        assertEquals(CommunityCategory.HOBBY, changed.category());
        assertEquals("새 제목", changed.title());
        assertEquals("새 내용", changed.content());
        assertEquals(author, changed.authorUserId()); // 보존
        assertEquals(1L, changed.viewCount()); // 보존
    }

    @Test
    void increaseViewCountAddsOne() {
        Post post = sample(UUID.randomUUID());

        assertEquals(2L, post.increaseViewCount().increaseViewCount().viewCount());
    }

    @Test
    void isAuthoredByChecksOwner() {
        UUID author = UUID.randomUUID();
        Post post = sample(author);

        assertTrue(post.isAuthoredBy(author));
        assertFalse(post.isAuthoredBy(UUID.randomUUID()));
    }

    @Test
    void rejectsBlankTitle() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> Post.create(CommunityCategory.FREE, " ", "내용", UUID.randomUUID()));
        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void rejectsNullCategory() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> Post.create(null, "제목", "내용", UUID.randomUUID()));
        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }
}
