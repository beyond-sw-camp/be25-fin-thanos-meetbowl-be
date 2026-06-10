package com.meetbowl.domain.community;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

import java.util.UUID;

/**
 * 익명 커뮤니티 게시글 도메인 모델이다(FR-119, FR-122).
 * 유저의 실제 아이디는 디비에 보관
 * 유저1이 글 작성  →  DB: authorUserId = 유저1의_UUID   (숨김)
 *  화면: 익명1(노출)
 *   - 화면에 보일 익명1, 익명2… 는 이 authorUserId로 CommunityAlias(매핑 테이블)를 조회해서 붙입니다.
 *   - Post는 진짜 ID만 들고, "익명 몇 번"인지는 CommunityAlias가 따로 책임 → 역할 분리
 *
 * <p>작성자는 {@code authorUserId}(실제 사용자)로 저장하되 화면에는 노출하지 않는다(수정/삭제 권한 확인용). 화면에 보이는 익명 표시명은 {@link
 * CommunityAlias}(사용자별 전역 고정 번호)를 조회해 붙인다. 좋아요/댓글 수와 인기글(Hot) 여부는 별도 집계/조회로 계산하며 이 모델이 소유하지 않는다.
 *
 * <p>불변 객체로 다루며 수정/조회수 증가는 새 인스턴스를 반환한다.
 */
public class Post {

    private final UUID id;

    /** 카테고리(필수). */
    private final CommunityCategory category;

    /** 제목(필수). */
    private final String title;

    /** 내용(필수). */
    private final String content;

    /** 실제 작성자(FK, 비공개). 익명 표시는 별도 매핑으로 처리. */
    private final UUID authorUserId;

    /** 조회수. */
    private final long viewCount;

    private Post(
            UUID id,
            CommunityCategory category,
            String title,
            String content,
            UUID authorUserId,
            long viewCount) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.content = content;
        this.authorUserId = authorUserId;
        this.viewCount = viewCount;
    }

    public static Post create(
            CommunityCategory category, String title, String content, UUID authorUserId) {
        return of(null, category, title, content, authorUserId, 0L);
    }

    public static Post of(
            UUID id,
            CommunityCategory category,
            String title,
            String content,
            UUID authorUserId,
            long viewCount) {
        if (category == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "카테고리는 필수입니다.");
        }
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "제목은 필수입니다.");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "내용은 필수입니다.");
        }
        if (authorUserId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "작성자는 필수입니다.");
        }
        if (viewCount < 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "조회수는 음수일 수 없습니다.");
        }
        return new Post(id, category, title, content, authorUserId, viewCount);
    }

    /** 게시글 수정(카테고리·제목·내용). 작성자·조회수는 보존한다. */
    public Post change(CommunityCategory newCategory, String newTitle, String newContent) {
        return of(id, newCategory, newTitle, newContent, authorUserId, viewCount);
    }

    /** 조회수 1 증가. */
    public Post increaseViewCount() {
        return of(id, category, title, content, authorUserId, viewCount + 1);
    }

    public boolean isAuthoredBy(UUID userId) {
        return authorUserId.equals(userId);
    }  // 숨긴 진짜 ID와 비교

    public UUID id() {
        return id;
    }

    public CommunityCategory category() {
        return category;
    }

    public String title() {
        return title;
    }

    public String content() {
        return content;
    }

    public UUID authorUserId() {
        return authorUserId;
    }

    public long viewCount() {
        return viewCount;
    }
}