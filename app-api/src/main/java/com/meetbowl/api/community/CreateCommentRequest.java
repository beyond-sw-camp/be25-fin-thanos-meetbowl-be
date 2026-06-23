package com.meetbowl.api.community;

import jakarta.validation.constraints.NotBlank;

/** 댓글 등록 요청 본문이다. 작성자(userId)는 본문으로 받지 않고 인증 토큰(@CurrentUser)에서만 확인한다. 대상 게시글은 경로 변수로 받는다. */
public record CreateCommentRequest(@NotBlank(message = "댓글 내용은 필수입니다.") String content) {}
