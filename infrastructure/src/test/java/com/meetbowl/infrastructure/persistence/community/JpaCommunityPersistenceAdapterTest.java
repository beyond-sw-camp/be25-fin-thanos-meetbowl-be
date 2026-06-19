package com.meetbowl.infrastructure.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import com.meetbowl.domain.common.Paged;
import com.meetbowl.domain.community.Comment;
import com.meetbowl.domain.community.CommentLike;
import com.meetbowl.domain.community.CommentLikeRepositoryPort;
import com.meetbowl.domain.community.CommentRepositoryPort;
import com.meetbowl.domain.community.CommunityAlias;
import com.meetbowl.domain.community.CommunityAliasRepositoryPort;
import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.PostLike;
import com.meetbowl.domain.community.PostLikeRepositoryPort;
import com.meetbowl.domain.community.PostRepositoryPort;
import com.meetbowl.infrastructure.config.InfrastructureConfig;

@SpringBootTest(classes = JpaCommunityPersistenceAdapterTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:community-jpa-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class JpaCommunityPersistenceAdapterTest {

    @Autowired private PostRepositoryPort postRepositoryPort;
    @Autowired private CommentRepositoryPort commentRepositoryPort;
    @Autowired private PostLikeRepositoryPort postLikeRepositoryPort;
    @Autowired private CommentLikeRepositoryPort commentLikeRepositoryPort;
    @Autowired private CommunityAliasRepositoryPort communityAliasRepositoryPort;

    @Test
    void postRoundTripPreservesValues() {
        UUID author = UUID.randomUUID();
        Post saved =
                postRepositoryPort.save(
                        Post.create(CommunityCategory.HOBBY, "제목", "내용입니다.", author));

        Post found = postRepositoryPort.findById(saved.id()).orElseThrow();

        assertThat(found.category()).isEqualTo(CommunityCategory.HOBBY);
        assertThat(found.title()).isEqualTo("제목");
        assertThat(found.content()).isEqualTo("내용입니다.");
        assertThat(found.authorUserId()).isEqualTo(author);
        assertThat(found.viewCount()).isZero();
    }

    @Test
    void findPageFiltersByCategory() {
        UUID author = UUID.randomUUID();
        postRepositoryPort.save(Post.create(CommunityCategory.FREE, "자유1", "내용", author));
        postRepositoryPort.save(Post.create(CommunityCategory.FREE, "자유2", "내용", author));
        postRepositoryPort.save(Post.create(CommunityCategory.RESTAURANT, "맛집1", "내용", author));

        Paged<Post> all = postRepositoryPort.findPage(null, 1, 10);
        Paged<Post> free = postRepositoryPort.findPage(CommunityCategory.FREE, 1, 10);

        assertThat(all.totalElements()).isEqualTo(3);
        assertThat(free.totalElements()).isEqualTo(2);
        assertThat(free.content()).extracting(Post::category).containsOnly(CommunityCategory.FREE);
    }

    @Test
    void commentSaveAndCountByPost() {
        UUID postId = UUID.randomUUID();
        UUID author = UUID.randomUUID();
        commentRepositoryPort.save(Comment.create(postId, "댓글1", author));
        commentRepositoryPort.save(Comment.create(postId, "댓글2", author));

        List<Comment> comments = commentRepositoryPort.findByPostId(postId);

        assertThat(comments).hasSize(2);
        assertThat(commentRepositoryPort.countByPostId(postId)).isEqualTo(2);
    }

    @Test
    void postLikeUniqueAndToggle() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        postLikeRepositoryPort.save(PostLike.create(postId, userId));

        assertThat(postLikeRepositoryPort.existsByPostIdAndUserId(postId, userId)).isTrue();
        assertThat(postLikeRepositoryPort.countByPostId(postId)).isEqualTo(1);

        // 같은 사용자가 같은 게시글에 중복 좋아요 → 유니크 제약 위반
        assertThatThrownBy(() -> postLikeRepositoryPort.save(PostLike.create(postId, userId)))
                .isInstanceOf(DataIntegrityViolationException.class);

        // 취소(토글)
        postLikeRepositoryPort.deleteByPostIdAndUserId(postId, userId);
        assertThat(postLikeRepositoryPort.existsByPostIdAndUserId(postId, userId)).isFalse();
        assertThat(postLikeRepositoryPort.countByPostId(postId)).isZero();
    }

    @Test
    void findLikedPostIdsReturnsOnlyRequestersLikesWithinGivenIds() {
        UUID user = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID likedPost = UUID.randomUUID();
        UUID notLikedPost = UUID.randomUUID();
        UUID likedByOther = UUID.randomUUID();

        // user는 likedPost만 좋아요, other는 likedByOther를 좋아요.
        postLikeRepositoryPort.save(PostLike.create(likedPost, user));
        postLikeRepositoryPort.save(PostLike.create(likedByOther, other));

        // 조회 대상 집합엔 안 누른 글·타인이 누른 글도 섞여 있지만, user가 누른 것만 돌려줘야 한다.
        Set<UUID> result =
                postLikeRepositoryPort.findLikedPostIds(
                        user, List.of(likedPost, notLikedPost, likedByOther));

        assertThat(result).containsExactly(likedPost);
    }

    @Test
    void findLikedPostIdsEmptyWhenNoIdsOrNoLikes() {
        UUID user = UUID.randomUUID();
        // 빈 ID 집합 → 쿼리 없이 빈 결과.
        assertThat(postLikeRepositoryPort.findLikedPostIds(user, List.of())).isEmpty();
        // 좋아요 이력이 없는 사용자 → 빈 결과.
        assertThat(postLikeRepositoryPort.findLikedPostIds(user, List.of(UUID.randomUUID())))
                .isEmpty();
    }

    @Test
    void findLikedCommentIdsReturnsOnlyRequestersLikesWithinGivenIds() {
        UUID user = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID likedComment = UUID.randomUUID();
        UUID notLikedComment = UUID.randomUUID();
        UUID likedByOther = UUID.randomUUID();

        commentLikeRepositoryPort.save(CommentLike.create(likedComment, user));
        commentLikeRepositoryPort.save(CommentLike.create(likedByOther, other));

        Set<UUID> result =
                commentLikeRepositoryPort.findLikedCommentIds(
                        user, List.of(likedComment, notLikedComment, likedByOther));

        assertThat(result).containsExactly(likedComment);
    }

    @Test
    void commentLikeRejectsDuplicate() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        commentLikeRepositoryPort.save(CommentLike.create(commentId, userId));

        assertThat(commentLikeRepositoryPort.existsByCommentIdAndUserId(commentId, userId))
                .isTrue();
        assertThatThrownBy(
                        () -> commentLikeRepositoryPort.save(CommentLike.create(commentId, userId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aliasRoundTripAndUniqueUser() {
        UUID userId = UUID.randomUUID();
        communityAliasRepositoryPort.save(CommunityAlias.create(userId, 1));

        CommunityAlias found = communityAliasRepositoryPort.findByUserId(userId).orElseThrow();
        assertThat(found.aliasNo()).isEqualTo(1);
        assertThat(found.displayName()).isEqualTo("익명1");

        // 같은 사용자에 익명 매핑 중복 → 유니크 제약 위반
        assertThatThrownBy(
                        () -> communityAliasRepositoryPort.save(CommunityAlias.create(userId, 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        CommunityJpaConfig.class,
        JpaPostRepositoryAdapter.class,
        JpaCommentRepositoryAdapter.class,
        JpaPostLikeRepositoryAdapter.class,
        JpaCommentLikeRepositoryAdapter.class,
        JpaCommunityAliasRepositoryAdapter.class
    })
    static class TestApplication {}
}
