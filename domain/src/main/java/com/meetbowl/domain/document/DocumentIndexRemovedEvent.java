package com.meetbowl.domain.document;

import java.util.UUID;

/** 문서(메모/파일 등)가 삭제되어 AI 검색 색인에서 제거해야 함을 알리는 도메인 이벤트다. */
public record DocumentIndexRemovedEvent(UUID documentId) {}
