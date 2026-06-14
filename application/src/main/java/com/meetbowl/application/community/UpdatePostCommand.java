package com.meetbowl.application.community;

import java.util.UUID;

/**
 * 게시글 수정 입력 모델이다. 수정 항목은 카테고리·제목·내용뿐이며, 조회수/좋아요수/댓글수와 작성자는 보존한다(수정 대상 아님).
 *
 * <p>{@code requesterId}는 인증된 요청자(@CurrentUser)다. 작성자 본인만 수정할 수 있는지 검증하는 데 쓰고, 응답에 노출하지 않는다. {@code
 * category}는 enum 이름("FREE") 또는 한글 라벨("자유") 문자열로 받아 UseCase에서 해석한다.
 */
public record UpdatePostCommand(
        UUID postId, UUID requesterId, String category, String title, String content) {}
