package com.meetbowl.domain.chatbot;

/** 챗봇 세션이 검색할 자료 범위를 표현한다. */
public enum ChatScopeType {
    /** 현재 사용자가 접근 가능한 모든 챗봇 자료를 대상으로 한다. 이때 scopeId는 사용하지 않는다. */
    GENERAL,

    /** 특정 회의 문맥으로 검색을 제한한다. */
    MEETING,

    /** 특정 회의록 문맥으로 검색을 제한한다. */
    MINUTES,

    /** 특정 개인 워크스페이스 문맥으로 검색을 제한한다. */
    PERSONAL_WORKSPACE,

    /** 특정 공유 워크스페이스 문맥으로 검색을 제한한다. */
    SHARED_WORKSPACE
}
