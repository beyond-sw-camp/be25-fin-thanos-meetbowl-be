package com.meetbowl.infrastructure.persistence.community;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.CommunityPostListItem;

/** Spring Data JPA 전용 repository다. application/domain 계층에서는 이 타입을 직접 참조하지 않는다. */
public interface SpringDataPostRepository extends JpaRepository<PostEntity, UUID> {

    Page<PostEntity> findByCategory(CommunityCategory category, Pageable pageable);

    /*
     * 목록 조회용 프로젝션 쿼리들이다. 게시글 한 건마다 좋아요/댓글 수를 함께 집계해 도메인 읽기 모델(CommunityPostListItem)로 바로 매핑한다.
     *
     * - 좋아요/댓글은 게시글과 연관 매핑(@OneToMany)이 없고 postId(raw UUID)로만 참조하므로, Hibernate의 엔티티 ON 조인으로 LEFT JOIN 한다.
     * - 두 자식 테이블을 동시에 LEFT JOIN 하면 행이 곱집합으로 늘어나므로 COUNT(DISTINCT ...)로 각각의 실제 개수를 센다.
     * - 카테고리/검색어는 선택 조건이다: 파라미터가 null이면 해당 필터를 적용하지 않는다(:keyword 는 어댑터에서 소문자 %like% 패턴으로 만들어 넘긴다).
     * - 페이징 정합성을 위해 정렬은 쿼리 안에 고정하고, Pageable 에는 정렬 없는 offset/limit 만 담아 전달한다.
     */

    String LIST_SELECT =
            "SELECT new com.meetbowl.domain.community.CommunityPostListItem("
                    + " p.id, p.category, p.title, p.content, p.authorUserId, p.viewCount,"
                    + " COUNT(DISTINCT l.id), COUNT(DISTINCT c.id), p.createdAt)"
                    + " FROM PostEntity p"
                    + " LEFT JOIN PostLikeEntity l ON l.postId = p.id"
                    + " LEFT JOIN CommentEntity c ON c.postId = p.id";

    // 공백 무시 검색: 저장된 제목/내용에서 공백을 제거하고 비교한다(검색어도 어댑터에서 공백 제거).
    // 예) 제목 "테 스트" ↔ 검색어 "테스트" 매칭. :keyword 는 소문자·공백제거 %like% 패턴이다.
    String LIST_FILTER =
            " WHERE (:category IS NULL OR p.category = :category)"
                    + " AND (:keyword IS NULL OR REPLACE(LOWER(p.title), ' ', '') LIKE :keyword"
                    + " OR REPLACE(LOWER(p.content), ' ', '') LIKE :keyword)";

    String LIST_GROUP_BY =
            " GROUP BY p.id, p.category, p.title, p.content, p.authorUserId, p.viewCount,"
                    + " p.createdAt";

    // 인기 점수식. 가중치(0.1/2/3)는 도메인 CommunityHotScore 와 동일하게 유지한다(가중치 변경 시 함께 수정).
    String SCORE_EXPR = "(p.viewCount * 0.1 + COUNT(DISTINCT l.id) * 2 + COUNT(DISTINCT c.id) * 3)";

    String LIST_COUNT =
            "SELECT COUNT(p) FROM PostEntity p"
                    + " WHERE (:category IS NULL OR p.category = :category)"
                    + " AND (:keyword IS NULL OR REPLACE(LOWER(p.title), ' ', '') LIKE :keyword"
                    + " OR REPLACE(LOWER(p.content), ' ', '') LIKE :keyword)";

    /** 최신순(createdAt DESC) 목록. */
    @Query(
            value = LIST_SELECT + LIST_FILTER + LIST_GROUP_BY + " ORDER BY p.createdAt DESC",
            countQuery = LIST_COUNT)
    Page<CommunityPostListItem> searchLatest(
            @Param("category") CommunityCategory category,
            @Param("keyword") String keyword,
            Pageable pageable);

    /** 인기순(전체 기간, 점수 DESC, 동률 시 최신 우선) 목록. */
    @Query(
            value =
                    LIST_SELECT
                            + LIST_FILTER
                            + LIST_GROUP_BY
                            + " ORDER BY "
                            + SCORE_EXPR
                            + " DESC, p.createdAt DESC",
            countQuery = LIST_COUNT)
    Page<CommunityPostListItem> searchPopular(
            @Param("category") CommunityCategory category,
            @Param("keyword") String keyword,
            Pageable pageable);

    /** Hot: since 이후 작성 글 중 점수 상위 N개(Pageable로 limit 지정). */
    @Query(
            LIST_SELECT
                    + " WHERE p.createdAt >= :since"
                    + LIST_GROUP_BY
                    + " ORDER BY "
                    + SCORE_EXPR
                    + " DESC, p.createdAt DESC")
    List<CommunityPostListItem> findHot(@Param("since") Instant since, Pageable pageable);

    /** 상세 화면용 단건 조회. 좋아요/댓글 수와 작성 시각을 함께 집계한다. */
    @Query(LIST_SELECT + " WHERE p.id = :postId" + LIST_GROUP_BY)
    Optional<CommunityPostListItem> findDetailById(@Param("postId") UUID postId);
}
