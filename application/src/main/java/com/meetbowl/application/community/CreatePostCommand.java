package com.meetbowl.application.community;

import java.util.UUID;

/**
 * 게시글 등록 입력 모델이다. {@code authorUserId}는 인증된 요청자를 app-api에서 채워 전달한다(본문으로 작성자를 받지 않아 사칭을 막는다). 익명 별칭은
 * 저장 시 작성자 기준으로 발급/재사용하며, 응답에는 실제 userId 대신 "익명N"만 노출한다.
 *
 * <p>{@code category}는 enum 이름("FREE") 또는 한글 라벨("자유") 문자열로 받는다. 카테고리 값 해석/검증은 UseCase에서 수행해
 * app-api가 도메인 타입에 의존하지 않도록 한다.
 */
public record CreatePostCommand(String category, String title, String content, UUID authorUserId) {}
