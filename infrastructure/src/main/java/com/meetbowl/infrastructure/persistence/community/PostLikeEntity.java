package com.meetbowl.infrastructure.persistence.community;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.community.PostLike;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 게시글 좋아요 JPA Entity다. {@code community_post_like} 테이블과 1:1로 매핑된다. (post_id, user_id) 유니크로 한 사용자의
 * 중복 좋아요를 차단한다.
 */
@Entity
@Table(
        name = "community_post_like",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_community_post_like_post_user",
                        columnNames = {"post_id", "user_id"}))
public class PostLikeEntity extends BaseEntity {

    /** 대상 게시글(FK). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID postId;

    /** 좋아요 누른 사용자(FK). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    protected PostLikeEntity() {}

    private PostLikeEntity(UUID postId, UUID userId) {
        this.postId = postId;
        this.userId = userId;
    }

    static PostLikeEntity from(PostLike postLike) {
        PostLikeEntity entity = new PostLikeEntity(postLike.postId(), postLike.userId());
        entity.setId(postLike.id());
        return entity;
    }

    PostLike toDomain() {
        return PostLike.of(getId(), postId, userId);
    }
}
