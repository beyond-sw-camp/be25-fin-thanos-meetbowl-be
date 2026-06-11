package com.meetbowl.domain.community;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 익명 커뮤니티 댓글 도메인 모델이다. 대댓글 없이 단일 레벨(게시글에 직접 달리는 댓글).
 *
 * <p>게시글은 {@code postId}, 작성자는 {@code authorUserId}(실제 사용자, 비공개) raw UUID로 참조한다. 화면 익명 표시명은 게시글과
 * 동일하게 {@link CommunityAlias}로 붙인다. 좋아요 수는 별도 집계한다.
 */
public class Comment {

    private final UUID id;

    /** 소속 게시글(FK). */
    private final UUID postId; // 어느 게시글의 댓글인지

    /** 내용(필수). */
    private final String content;

    /** 실제 작성자(FK, 비공개). */
    private final UUID authorUserId;

    private Comment(UUID id, UUID postId, String content, UUID authorUserId) {
        this.id = id;
        this.postId = postId;
        this.content = content;
        this.authorUserId = authorUserId;
    }

    public static Comment create(UUID postId, String content, UUID authorUserId) {
        return of(null, postId, content, authorUserId);
    }

    public static Comment of(UUID id, UUID postId, String content, UUID authorUserId) {
        if (postId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "게시글은 필수입니다.");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "댓글 내용은 필수입니다.");
        }
        if (authorUserId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "작성자는 필수입니다.");
        }
        return new Comment(id, postId, content, authorUserId);
    }

    /** 댓글 수정(내용). */
    public Comment change(String newContent) {
        return of(id, postId, newContent, authorUserId);
    }

    public boolean isAuthoredBy(UUID userId) {
        return authorUserId.equals(userId);
    }

    public UUID id() {
        return id;
    }

    public UUID postId() {
        return postId;
    }

    public String content() {
        return content;
    }

    public UUID authorUserId() {
        return authorUserId;
    }
}
