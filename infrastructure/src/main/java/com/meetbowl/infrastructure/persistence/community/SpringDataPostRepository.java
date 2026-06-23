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

    /*
     * 검색 정규화 표현식이다. 소문자화 후 공백/하이픈(-)/슬래시(/)를 모두 제거해 비교한다(검색어도 어댑터 toLikePattern 에서 동일하게 정규화한다).
     * 예) 제목 "테 스트" ↔ 검색어 "테스트", "스프링-핫글" ↔ "스프링 핫글" 매칭.
     *
     * 한계: 정규화로 가공한 값을 LIKE 비교하므로 제목/내용 컬럼 인덱스를 타지 못한다(Full Scan). 게시글이 크게 늘면 검색이 느려지므로,
     * 그 시점에는 MySQL Full-text(N-gram 파서)나 검색엔진(Elasticsearch) 도입 또는 정규화 컬럼 사전 저장으로 고도화한다.
     * 구두점/이음동의어(예: 슬래시·하이픈 외 기호, 유의어)까지는 의도적으로 다루지 않는다(현재 규모에서는 공백/하이픈/슬래시 무시로 충분).
     */
    String NORM_TITLE = "REPLACE(REPLACE(REPLACE(LOWER(p.title), ' ', ''), '-', ''), '/', '')";
    String NORM_CONTENT = "REPLACE(REPLACE(REPLACE(LOWER(p.content), ' ', ''), '-', ''), '/', '')";

    String LIST_FILTER =
            " WHERE (:category IS NULL OR p.category = :category)"
                    + " AND (:keyword IS NULL OR "
                    + NORM_TITLE
                    + " LIKE :keyword OR "
                    + NORM_CONTENT
                    + " LIKE :keyword)";

    String LIST_GROUP_BY =
            " GROUP BY p.id, p.category, p.title, p.content, p.authorUserId, p.viewCount,"
                    + " p.createdAt";

    // 인기 점수식. 가중치(0.1/2/3)는 도메인 CommunityHotScore 와 동일하게 유지한다(가중치 변경 시 함께 수정).
    String SCORE_EXPR = "(p.viewCount * 0.1 + COUNT(DISTINCT l.id) * 2 + COUNT(DISTINCT c.id) * 3)";

    String LIST_COUNT =
            "SELECT COUNT(p) FROM PostEntity p"
                    + " WHERE (:category IS NULL OR p.category = :category)"
                    + " AND (:keyword IS NULL OR "
                    + NORM_TITLE
                    + " LIKE :keyword OR "
                    + NORM_CONTENT
                    + " LIKE :keyword)";

    // Hot 게시글 목록: 좋아요 수가 임계값 이상인 글만 남긴다. likeCount 는 저장 컬럼이 아닌 집계값이라
    // WHERE 가 아닌 HAVING 으로 거른다(메모리 필터 X). 임계값은 도메인 CommunityHotScore.HOT_LIKE_THRESHOLD.
    // category/keyword 검색은 일반 목록과 동일하게 LIST_FILTER 로 함께 적용한다.
    String LIST_HOT_HAVING = " HAVING COUNT(DISTINCT l.id) >= :minLikeCount";

    // Hot 목록 페이징용 카운트. category/keyword 필터에 더해, 좋아요가 임계값 이상인 게시글만 센다.
    // 게시글마다 좋아요 테이블을 다시 세는 상관 서브쿼리(O(글 수 × 좋아요 스캔)) 대신, 좋아요를 postId로 한 번만
    // GROUP BY/HAVING 집계해 '임계값 이상' 게시글 ID 집합을 만들고 IN 으로 거른다(좋아요 집계 1회, post_id 인덱스 활용).
    String LIST_HOT_COUNT =
            "SELECT COUNT(p) FROM PostEntity p"
                    + " WHERE (:category IS NULL OR p.category = :category)"
                    + " AND (:keyword IS NULL OR "
                    + NORM_TITLE
                    + " LIKE :keyword OR "
                    + NORM_CONTENT
                    + " LIKE :keyword)"
                    + " AND p.id IN (SELECT pl.postId FROM PostLikeEntity pl"
                    + " GROUP BY pl.postId HAVING COUNT(pl.id) >= :minLikeCount)";

    /** 최신순(createdAt DESC) 목록. */
    @Query(
            value = LIST_SELECT + LIST_FILTER + LIST_GROUP_BY + " ORDER BY p.createdAt DESC",
            countQuery = LIST_COUNT)
    Page<CommunityPostListItem> searchLatest(
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

    /** Hot 게시글 목록: 좋아요 minLikeCount개 이상(+선택 category/keyword), 최신순, 페이징. */
    @Query(
            value =
                    LIST_SELECT
                            + LIST_FILTER
                            + LIST_GROUP_BY
                            + LIST_HOT_HAVING
                            + " ORDER BY p.createdAt DESC",
            countQuery = LIST_HOT_COUNT)
    Page<CommunityPostListItem> searchHotByLikes(
            @Param("category") CommunityCategory category,
            @Param("keyword") String keyword,
            @Param("minLikeCount") int minLikeCount,
            Pageable pageable);

    /** 상세 화면용 단건 조회. 좋아요/댓글 수와 작성 시각을 함께 집계한다. */
    @Query(LIST_SELECT + " WHERE p.id = :postId" + LIST_GROUP_BY)
    Optional<CommunityPostListItem> findDetailById(@Param("postId") UUID postId);
}
