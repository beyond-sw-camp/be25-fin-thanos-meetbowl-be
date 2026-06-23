package com.meetbowl.domain.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** 게시글/댓글 좋아요 도메인 모델의 필수값 검증을 확인한다. */
class CommunityLikeTest {

    @Test
    void postLikeRejectsNullPost() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> PostLike.create(null, UUID.randomUUID()));
        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void postLikeRejectsNullUser() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> PostLike.create(UUID.randomUUID(), null));
        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void commentLikeRejectsNullComment() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> CommentLike.create(null, UUID.randomUUID()));
        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void commentLikeRejectsNullUser() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> CommentLike.create(UUID.randomUUID(), null));
        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }
}
