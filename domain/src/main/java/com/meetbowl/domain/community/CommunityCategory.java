package com.meetbowl.domain.community;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 익명 커뮤니티 게시글 카테고리다. 글 작성 시 아래 값 중 하나를 선택해 저장한다. 목록의 "전체"는 카테고리 값이 아니라 필터 미적용(전체 조회)을 의미하므로 여기에
 * 포함하지 않는다.
 *
 * <p>각 상수가 생성자에 "카테고리명" 을 넘김
 */
public enum CommunityCategory {
    FREE("자유"),
    COMPANY_LIFE("회사생활"),
    HOBBY("취미"),
    RESTAURANT("맛집");

    private final String label; // 각 상수가 들고 있을 한글 이름

    CommunityCategory(String label) {
        this.label = label;
    } // 생성자 — 위 "자유" 등을 받아서 label 필드에 저장

    public String label() {
        return label;
    } // 저장된 걸 꺼내온다.

    /**
     * 클라이언트 입력값을 카테고리로 해석한다. enum 이름(예: {@code "FREE"}, 대소문자 무시)과 한글 라벨(예: {@code "자유"})을 모두 받아준다.
     * FE가 화면에 보이는 한글 라벨을 그대로 보내는 경우와 안정적인 코드값을 보내는 경우를 함께 수용하기 위함이다. 알 수 없는 값은 잘못된 요청(400)으로 처리한다.
     */
    public static CommunityCategory from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "카테고리는 필수입니다.");
        }
        String normalized = value.trim();
        for (CommunityCategory category : values()) {
            if (category.name().equalsIgnoreCase(normalized) || category.label.equals(normalized)) {
                return category;
            }
        }
        throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "알 수 없는 카테고리입니다: " + value);
    }
}
