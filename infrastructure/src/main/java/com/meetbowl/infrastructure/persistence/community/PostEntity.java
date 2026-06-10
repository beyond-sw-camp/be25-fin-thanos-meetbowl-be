package com.meetbowl.infrastructure.persistence.community;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.Post;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 게시글 JPA Entity다. {@code community_post} 테이블과 1:1로 매핑된다. 작성자는 raw UUID로 참조하며 익명 표시명은 별도 매핑({@link
 * CommunityAliasEntity})으로 처리한다.
 */
@Entity
@Table(
        name = "community_post",
        indexes = {@Index(name = "idx_community_post_category", columnList = "category")})
public class PostEntity extends BaseEntity {

    /** 카테고리. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityCategory category;

    /** 제목. */
    @Column(nullable = false, length = 200)
    private String title;

    /** 내용. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 실제 작성자(FK, 비공개). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID authorUserId;

    /** 조회수. */
    @Column(nullable = false)
    private long viewCount;

    protected PostEntity() {}

    private PostEntity(
            CommunityCategory category,
            String title,
            String content,
            UUID authorUserId,
            long viewCount) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.authorUserId = authorUserId;
        this.viewCount = viewCount;
    }

    static PostEntity from(Post post) {
        PostEntity entity =
                new PostEntity(
                        post.category(),
                        post.title(),
                        post.content(),
                        post.authorUserId(),
                        post.viewCount());
        entity.setId(post.id());
        return entity;
    }

    Post toDomain() {
        return Post.of(getId(), category, title, content, authorUserId, viewCount);
    }
}