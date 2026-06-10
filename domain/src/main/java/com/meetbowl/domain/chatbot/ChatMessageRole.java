package com.meetbowl.domain.chatbot;

/** 챗봇 메시지를 작성한 주체를 구분한다. */
public enum ChatMessageRole {
    /** 로그인 사용자가 입력한 질문 또는 후속 메시지다. */
    USER,

    /** LLM이 생성한 답변이다. */
    ASSISTANT,

    /** 대화 문맥이나 운영 지시를 기록하기 위한 시스템 메시지다. */
    SYSTEM
}
