package com.meetbowl.api.community;

import jakarta.validation.constraints.NotBlank;

/** 댓글 수정 요청 본문이다. 수정 항목은 내용뿐이다. 작성자(userId)는 본문으로 받지 않고 인증 토큰(@CurrentUser)에서만 확인한다. */
public record UpdateCommentRequest(@NotBlank(message = "댓글 내용은 필수입니다.") String content) {}
