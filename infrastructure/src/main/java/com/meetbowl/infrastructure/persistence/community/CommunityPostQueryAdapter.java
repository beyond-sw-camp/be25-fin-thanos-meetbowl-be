package com.meetbowl.infrastructure.persistence.community;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.meetbowl.domain.common.Paged;
import com.meetbowl.domain.community.CommunityHotScore;
import com.meetbowl.domain.community.CommunityPostListItem;
import com.meetbowl.domain.community.CommunityPostQuery;
import com.meetbowl.domain.community.CommunityPostQueryPort;

/**
 * 커뮤니티 게시글 조회 전용 포트의 JPA 구현 adapter다. 검색/Hot 조건을 Spring Data 프로젝션 쿼리로 위임하고, 도메인 조회 모델로의 변환은 이
 * 경계에서만 수행한다.
 */
@Repository
public class CommunityPostQueryAdapter implements CommunityPostQueryPort {

    private final SpringDataPostRepository springDataPostRepository;

    public CommunityPostQueryAdapter(SpringDataPostRepository springDataPostRepository) {
        this.springDataPostRepository = springDataPostRepository;
    }

    @Override
    public Paged<CommunityPostListItem> search(CommunityPostQuery query) {
        // 정렬은 쿼리 안에 고정돼 있으므로 Pageable 에는 정렬 없이 offset/limit 만 담는다(page는 1부터 → 0 기반으로 변환).
        Pageable pageable = PageRequest.of(query.page() - 1, query.size());
        String keyword = toLikePattern(query.keyword());

        Page<CommunityPostListItem> result;
        if (query.hotOnly()) {
            // Hot 목록: 좋아요 임계값 이상만(HAVING). category/keyword 는 함께 적용, 정렬은 최신순 고정.
            result =
                    springDataPostRepository.searchHotByLikes(
                            query.category(),
                            keyword,
                            CommunityHotScore.HOT_LIKE_THRESHOLD,
                            pageable);
        } else {
            result = springDataPostRepository.searchLatest(query.category(), keyword, pageable);
        }

        return new Paged<>(result.getContent(), result.getTotalElements());
    }

    @Override
    public Optional<CommunityPostListItem> findById(UUID postId) {
        return springDataPostRepository.findDetailById(postId);
    }

    @Override
    public List<CommunityPostListItem> findHot(Instant since, int limit) {
        // Hot 은 상위 N개만 필요하므로 첫 페이지에 limit 만큼만 요청한다.
        return springDataPostRepository.findHot(since, PageRequest.of(0, limit));
    }

    /**
     * 검색어를 대소문자·공백 무시 부분일치 LIKE 패턴으로 정규화한다. 앞뒤 공백 제거 → 소문자 변환 → 내부 공백까지 모두 제거한 뒤 {@code %...%} 로
     * 감싼다. 쿼리도 제목/내용의 공백을 제거하고 비교하므로 "테 스트" ↔ "테스트" 가 매칭된다. null/공백이면 검색 미적용을 의미하는 null 을 반환한다(쿼리의
     * {@code :keyword IS NULL} 분기와 연결).
     */
    private String toLikePattern(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return "%" + trimmed.toLowerCase().replace(" ", "") + "%";
    }
}
