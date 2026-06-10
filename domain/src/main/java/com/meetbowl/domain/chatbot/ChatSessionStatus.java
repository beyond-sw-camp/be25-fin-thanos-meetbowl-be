package com.meetbowl.domain.chatbot;

/** 챗봇 세션의 사용자 노출 및 사용 가능 상태다. */
public enum ChatSessionStatus {
    /** 사용자가 계속 메시지를 추가하고 조회할 수 있는 세션이다. 자동 만료하지 않는다. */
    ACTIVE,

    /** 사용자가 삭제한 세션이다. 이력 보존을 위해 soft delete 상태로 관리한다. */
    DELETED
}
