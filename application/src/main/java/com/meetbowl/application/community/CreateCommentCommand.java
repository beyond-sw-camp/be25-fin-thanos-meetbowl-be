package com.meetbowl.application.community;

import java.util.UUID;

/**
 * 댓글 등록 입력 모델이다. {@code authorUserId}는 인증된 요청자(@CurrentUser)를 app-api에서 채워 전달한다(본문으로 작성자를 받지 않아 사칭을
 * 막는다).
 *
 * <p>익명 별칭은 게시글과 동일한 매핑을 재사용한다: 작성자가 이미 별칭이 있으면 그 번호로, 댓글이 첫 활동이면 새로 발급해 "익명N"으로 표시한다. 응답에는 실제
 * userId를 노출하지 않는다.
 */
public record CreateCommentCommand(UUID postId, String content, UUID authorUserId) {}
