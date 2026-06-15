package com.meetbowl.infrastructure.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.community.Comment;
import com.meetbowl.domain.community.CommentLike;
import com.meetbowl.domain.community.CommentLikeRepositoryPort;
import com.meetbowl.domain.community.CommentRepositoryPort;
import com.meetbowl.domain.community.CommunityCommentListItem;
import com.meetbowl.domain.community.CommunityCommentQueryPort;
import com.meetbowl.infrastructure.config.InfrastructureConfig;

/** 댓글 조회 전용 어댑터의 쿼리 실행을 검증한다. 핵심은 댓글별 좋아요 수를 한 번에 집계해 읽기 모델로 매핑하는지와 등록순 정렬이다. */
@SpringBootTest(classes = CommunityCommentQueryAdapterTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:community-comment-query-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
// 각 테스트를 트랜잭션으로 감싸 롤백한다(공유 in-memory DB의 메서드 간 누적 방지).
@Transactional
class CommunityCommentQueryAdapterTest {

    @Autowired private CommentRepositoryPort commentRepositoryPort;
    @Autowired private CommentLikeRepositoryPort commentLikeRepositoryPort;
    @Autowired private CommunityCommentQueryPort communityCommentQueryPort;

    @Test
    void listsCommentsInRegistrationOrderWithLikeCounts() {
        UUID postId = UUID.randomUUID();
        UUID author = UUID.randomUUID();
        Comment first = commentRepositoryPort.save(Comment.create(postId, "첫 댓글", author));
        Comment second = commentRepositoryPort.save(Comment.create(postId, "둘째 댓글", author));
        // 다른 게시글 댓글은 섞이면 안 된다.
        commentRepositoryPort.save(Comment.create(UUID.randomUUID(), "다른 글 댓글", author));

        // first 댓글에 좋아요 2건(서로 다른 사용자), second 댓글엔 0건.
        commentLikeRepositoryPort.save(CommentLike.create(first.id(), UUID.randomUUID()));
        commentLikeRepositoryPort.save(CommentLike.create(first.id(), UUID.randomUUID()));

        List<CommunityCommentListItem> items = communityCommentQueryPort.findByPostId(postId);

        // 해당 게시글 댓글만, 등록순(first → second)으로.
        assertThat(items)
                .extracting(CommunityCommentListItem::id)
                .containsExactly(first.id(), second.id());
        assertThat(items.get(0).likeCount()).isEqualTo(2);
        assertThat(items.get(0).content()).isEqualTo("첫 댓글");
        assertThat(items.get(0).createdAt()).isNotNull();
        assertThat(items.get(1).likeCount()).isZero();
    }

    @Test
    void returnsEmptyWhenNoComments() {
        assertThat(communityCommentQueryPort.findByPostId(UUID.randomUUID())).isEmpty();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        CommunityJpaConfig.class,
        JpaCommentRepositoryAdapter.class,
        JpaCommentLikeRepositoryAdapter.class,
        CommunityCommentQueryAdapter.class
    })
    static class TestApplication {}
}
