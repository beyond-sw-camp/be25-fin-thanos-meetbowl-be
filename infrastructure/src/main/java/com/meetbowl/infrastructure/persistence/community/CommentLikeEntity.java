package com.meetbowl.infrastructure.persistence.community;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.community.CommentLike;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 댓글 좋아요 JPA Entity다. {@code community_comment_like} 테이블과 1:1로 매핑된다. (comment_id, user_id) 유니크로 한
 * 사용자의 중복 좋아요를 차단한다.
 */
@Entity
@Table(
        name = "community_comment_like",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_community_comment_like_comment_user",
                        columnNames = {"comment_id", "user_id"}))
public class CommentLikeEntity extends BaseEntity {

    /** 대상 댓글(FK). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID commentId;

    /** 좋아요 누른 사용자(FK). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    protected CommentLikeEntity() {}

    private CommentLikeEntity(UUID commentId, UUID userId) {
        this.commentId = commentId;
        this.userId = userId;
    }

    static CommentLikeEntity from(CommentLike commentLike) {
        CommentLikeEntity entity =
                new CommentLikeEntity(commentLike.commentId(), commentLike.userId());
        entity.setId(commentLike.id());
        return entity;
    }

    CommentLike toDomain() {
        return CommentLike.of(getId(), commentId, userId);
    }
}
