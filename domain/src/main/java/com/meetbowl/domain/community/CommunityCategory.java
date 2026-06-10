package com.meetbowl.domain.community;

/**
 * 익명 커뮤니티 게시글 카테고리다. 글 작성 시 아래 값 중 하나를 선택해 저장한다. 목록의 "전체"는 카테고리 값이 아니라 필터 미적용(전체 조회)을 의미하므로 여기에
 * 포함하지 않는다.

 * 각 상수가 생성자에 "카테고리명" 을 넘김
 */
public enum CommunityCategory {
    FREE("자유"),
    COMPANY_LIFE("회사생활"),
    HOBBY("취미"),
    RESTAURANT("맛집");

    private final String label;  // 각 상수가 들고 있을 한글 이름

    CommunityCategory(String label) {
        this.label = label;
    } // 생성자 — 위 "자유" 등을 받아서 label 필드에 저장

    public String label() {
        return label;
    } // 저장된 걸 꺼내온다.
}
