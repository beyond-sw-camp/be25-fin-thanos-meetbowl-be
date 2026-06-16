package com.meetbowl.api.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 게시글 수정 요청 본문이다. 수정 항목은 카테고리·제목·내용뿐이며, 조회수/좋아요수/댓글수는 수정 대상이 아니라 본문에 포함하지 않는다. 작성자(userId)는 본문으로 받지
 * 않고 인증 토큰(@CurrentUser)에서만 확인한다.
 *
 * <p>{@code category}는 enum 이름("FREE") 또는 한글 라벨("자유") 문자열로 받아 UseCase에서 해석한다(알 수 없는 값은 400).
 */
public record UpdatePostRequest(
        @NotBlank(message = "카테고리는 필수입니다.") String category,
        @NotBlank(message = "제목은 필수입니다.") @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
                String title,
        @NotBlank(message = "내용은 필수입니다.") String content) {}
