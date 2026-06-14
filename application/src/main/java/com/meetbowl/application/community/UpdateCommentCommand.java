package com.meetbowl.application.community;

import java.util.UUID;

/**
 * 댓글 수정 입력 모델이다. 수정 항목은 내용뿐이며, 작성자·좋아요 수는 보존한다(수정 대상 아님).
 *
 * <p>{@code requesterId}는 인증된 요청자(@CurrentUser)다. 작성자 본인만 수정할 수 있는지 검증하는 데 쓰고 응답에 노출하지 않는다. {@code
 * postId}는 경로의 게시글로, 댓글이 그 게시글에 속하는지 확인하는 데 쓴다.
 */
public record UpdateCommentCommand(UUID postId, UUID commentId, UUID requesterId, String content) {}
