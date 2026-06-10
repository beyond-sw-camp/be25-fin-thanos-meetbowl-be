package com.meetbowl.infrastructure.persistence.community;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.meetbowl.domain.community.Comment;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 댓글 JPA Entity다. {@code community_comment} 테이블과 1:1로 매핑된다. 게시글/작성자는 raw UUID로 참조하고, post_id 인덱스로 게시글별
 * 댓글 조회를 지원한다.
 */
@Entity
@Table(
        name = "community_comment",
        indexes = {@Index(name = "idx_community_comment_post", columnList = "post_id")})
public class CommentEntity extends BaseEntity {

    /** 소속 게시글(FK). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID postId;

    /** 내용. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 실제 작성자(FK, 비공개). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID authorUserId;

    protected CommentEntity() {}

    private CommentEntity(UUID postId, String content, UUID authorUserId) {
        this.postId = postId;
        this.content = content;
        this.authorUserId = authorUserId;
    }

    static CommentEntity from(Comment comment) {
        CommentEntity entity =
                new CommentEntity(comment.postId(), comment.content(), comment.authorUserId());
        entity.setId(comment.id());
        return entity;
    }

    Comment toDomain() {
        return Comment.of(getId(), postId, content, authorUserId);
    }
}