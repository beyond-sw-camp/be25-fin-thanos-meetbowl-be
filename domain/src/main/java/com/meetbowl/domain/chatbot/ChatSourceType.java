package com.meetbowl.domain.chatbot;

/**
 * AI 챗봇이 RAG 검색과 답변 근거로 사용할 수 있는 자료 유형이다.
 *
 * <p>이 값은 citation 저장과 AI 서버 계약에 사용한다. 실제 검색 권한은 자료 유형만으로 판단하지 않고, 질문 시점에 인증 사용자와 공유 워크스페이스 멤버십을 다시
 * 확인한다.
 */
public enum ChatSourceType {
    /** 사용자가 수동 또는 자동으로 개인 워크스페이스에 백업한 메일이다. */
    BACKUP_MAIL,

    /** 사용자가 Host 또는 Participant이고 상태가 APPROVED 또는 SHARED인 회의록이다. */
    MINUTES,

    /** 현재 사용자가 소유한 개인 메모다. */
    PERSONAL_MEMO,

    /** 현재 사용자가 소유한 개인 드라이브 파일이다. 개인 드라이브는 버전을 관리하지 않는다. */
    PERSONAL_DRIVE_FILE,

    /** 현재 사용자가 Owner 또는 Member인 공유 워크스페이스의 파일 버전이다. */
    SHARED_WORKSPACE_FILE_VERSION,
}
