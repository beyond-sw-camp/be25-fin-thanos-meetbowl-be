package com.meetbowl.infrastructure.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.common.Paged;
import com.meetbowl.domain.community.Comment;
import com.meetbowl.domain.community.CommentRepositoryPort;
import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.CommunityPostListItem;
import com.meetbowl.domain.community.CommunityPostQuery;
import com.meetbowl.domain.community.CommunityPostQueryPort;
import com.meetbowl.domain.community.CommunityPostSort;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.PostLike;
import com.meetbowl.domain.community.PostLikeRepositoryPort;
import com.meetbowl.domain.community.PostRepositoryPort;
import com.meetbowl.infrastructure.config.InfrastructureConfig;

/**
 * 게시글 조회 전용 어댑터의 쿼리 실행을 검증한다. 핵심은 (1) 좋아요/댓글 수를 한 번에 집계해 읽기 모델로 매핑하는지, (2) 인기 점수 정렬/검색/카테고리 필터/Hot
 * 한정이 의도대로 동작하는지다.
 */
@SpringBootTest(classes = CommunityPostQueryAdapterTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:community-query-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
// 각 테스트를 트랜잭션으로 감싸 롤백한다. 같은 in-memory DB를 공유하는 메서드 간 데이터 누적을 막아 카운트/정렬 단언을 결정적으로 만든다.
@Transactional
class CommunityPostQueryAdapterTest {

    @Autowired private PostRepositoryPort postRepositoryPort;
    @Autowired private PostLikeRepositoryPort postLikeRepositoryPort;
    @Autowired private CommentRepositoryPort commentRepositoryPort;
    @Autowired private CommunityPostQueryPort communityPostQueryPort;

    @Test
    void searchAggregatesLikeAndCommentCounts() {
        UUID author = UUID.randomUUID();
        Post post =
                postRepositoryPort.save(Post.create(CommunityCategory.FREE, "집계 대상", "내용", author));
        // 좋아요 2건(서로 다른 사용자), 댓글 3건.
        postLikeRepositoryPort.save(PostLike.create(post.id(), UUID.randomUUID()));
        postLikeRepositoryPort.save(PostLike.create(post.id(), UUID.randomUUID()));
        commentRepositoryPort.save(Comment.create(post.id(), "c1", UUID.randomUUID()));
        commentRepositoryPort.save(Comment.create(post.id(), "c2", UUID.randomUUID()));
        commentRepositoryPort.save(Comment.create(post.id(), "c3", UUID.randomUUID()));

        Paged<CommunityPostListItem> page =
                communityPostQueryPort.search(
                        new CommunityPostQuery(null, null, CommunityPostSort.LATEST, 1, 10));

        assertThat(page.totalElements()).isEqualTo(1);
        CommunityPostListItem item = page.content().get(0);
        assertThat(item.likeCount()).isEqualTo(2);
        assertThat(item.commentCount()).isEqualTo(3);
        assertThat(item.authorUserId()).isEqualTo(author);
        assertThat(item.createdAt()).isNotNull();
    }

    @Test
    void popularSortOrdersByScoreDescending() {
        UUID author = UUID.randomUUID();
        // 낮은 점수: 좋아요 1건만 → score 2.
        Post low =
                postRepositoryPort.save(Post.create(CommunityCategory.FREE, "낮은 점수", "내용", author));
        postLikeRepositoryPort.save(PostLike.create(low.id(), UUID.randomUUID()));
        // 높은 점수: 댓글 3건 → score 9.
        Post high =
                postRepositoryPort.save(Post.create(CommunityCategory.FREE, "높은 점수", "내용", author));
        commentRepositoryPort.save(Comment.create(high.id(), "c1", UUID.randomUUID()));
        commentRepositoryPort.save(Comment.create(high.id(), "c2", UUID.randomUUID()));
        commentRepositoryPort.save(Comment.create(high.id(), "c3", UUID.randomUUID()));

        List<CommunityPostListItem> items =
                communityPostQueryPort
                        .search(
                                new CommunityPostQuery(
                                        null, null, CommunityPostSort.POPULAR, 1, 10))
                        .content();

        assertThat(items)
                .extracting(CommunityPostListItem::id)
                .containsExactly(high.id(), low.id());
    }

    @Test
    void keywordMatchesTitleOrContentCaseInsensitive() {
        UUID author = UUID.randomUUID();
        postRepositoryPort.save(Post.create(CommunityCategory.FREE, "Spring 정리", "본문", author));
        postRepositoryPort.save(Post.create(CommunityCategory.FREE, "기타", "SPRING 내용 포함", author));
        postRepositoryPort.save(Post.create(CommunityCategory.FREE, "무관", "관계없음", author));

        Paged<CommunityPostListItem> page =
                communityPostQueryPort.search(
                        new CommunityPostQuery(null, "spring", CommunityPostSort.LATEST, 1, 10));

        // 제목 또는 본문에 spring(대소문자 무시) 포함된 2건만.
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void categoryFilterRestrictsResults() {
        UUID author = UUID.randomUUID();
        postRepositoryPort.save(Post.create(CommunityCategory.FREE, "자유", "내용", author));
        postRepositoryPort.save(Post.create(CommunityCategory.RESTAURANT, "맛집", "내용", author));

        Paged<CommunityPostListItem> page =
                communityPostQueryPort.search(
                        new CommunityPostQuery(
                                CommunityCategory.RESTAURANT,
                                null,
                                CommunityPostSort.LATEST,
                                1,
                                10));

        assertThat(page.content())
                .extracting(CommunityPostListItem::category)
                .containsOnly(CommunityCategory.RESTAURANT);
    }

    @Test
    void findHotReturnsTopScoredWithinLimit() {
        UUID author = UUID.randomUUID();
        // 댓글 수가 많을수록 점수가 높다. 상위 2개만 요청해 점수 상위 글이 선택되는지 본다.
        Post a = postRepositoryPort.save(Post.create(CommunityCategory.FREE, "A", "내용", author));
        Post b = postRepositoryPort.save(Post.create(CommunityCategory.FREE, "B", "내용", author));
        Post c = postRepositoryPort.save(Post.create(CommunityCategory.FREE, "C", "내용", author));
        // A: 댓글 1, B: 댓글 3, C: 댓글 2.
        commentRepositoryPort.save(Comment.create(a.id(), "a1", UUID.randomUUID()));
        for (int i = 0; i < 3; i++) {
            commentRepositoryPort.save(Comment.create(b.id(), "b" + i, UUID.randomUUID()));
        }
        commentRepositoryPort.save(Comment.create(c.id(), "c1", UUID.randomUUID()));
        commentRepositoryPort.save(Comment.create(c.id(), "c2", UUID.randomUUID()));

        Instant since = Instant.now().minus(48, ChronoUnit.HOURS);
        List<CommunityPostListItem> hot = communityPostQueryPort.findHot(since, 2);

        // 점수 상위 2개(B=9, C=6)가 순서대로.
        assertThat(hot).extracting(CommunityPostListItem::id).containsExactly(b.id(), c.id());
    }

    @Test
    void findByIdReturnsSinglePostWithCounts() {
        UUID author = UUID.randomUUID();
        Post post =
                postRepositoryPort.save(Post.create(CommunityCategory.FREE, "단건", "본문", author));
        postLikeRepositoryPort.save(PostLike.create(post.id(), UUID.randomUUID()));
        commentRepositoryPort.save(Comment.create(post.id(), "댓글", UUID.randomUUID()));

        Optional<CommunityPostListItem> found = communityPostQueryPort.findById(post.id());

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(post.id());
        assertThat(found.get().likeCount()).isEqualTo(1);
        assertThat(found.get().commentCount()).isEqualTo(1);
        assertThat(found.get().createdAt()).isNotNull();
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        assertThat(communityPostQueryPort.findById(UUID.randomUUID())).isEmpty();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        CommunityJpaConfig.class,
        JpaPostRepositoryAdapter.class,
        JpaCommentRepositoryAdapter.class,
        JpaPostLikeRepositoryAdapter.class,
        CommunityPostQueryAdapter.class
    })
    static class TestApplication {}
}
