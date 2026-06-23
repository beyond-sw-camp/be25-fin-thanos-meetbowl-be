package com.meetbowl.api.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 게시글 등록 요청 본문이다. 작성자(userId)는 본문으로 받지 않고 인증 토큰(@CurrentUser)에서만 채워 사칭을 막는다.
 *
 * <p>{@code category}는 enum 이름("FREE") 또는 한글 라벨("자유") 문자열로 받아 컨트롤러에서 {@link
 * com.meetbowl.domain.community.CommunityCategory#from}로 해석한다(알 수 없는 값은 400). 빈 값 검증은 여기서, 카테고리 값
 * 유효성은 변환 시점에 처리한다.
 */
public record CreatePostRequest(
        @NotBlank(message = "카테고리는 필수입니다.") String category,
        @NotBlank(message = "제목은 필수입니다.") @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
                String title,
        @NotBlank(message = "내용은 필수입니다.") String content) {}
