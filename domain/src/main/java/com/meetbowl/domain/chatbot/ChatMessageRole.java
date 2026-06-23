package com.meetbowl.domain.chatbot;

/** 외부 대화 이력에는 신뢰할 수 없는 시스템 지시가 섞이지 않도록 사용자와 AI 역할만 허용한다. */
public enum ChatMessageRole {
    USER,
    ASSISTANT
}
