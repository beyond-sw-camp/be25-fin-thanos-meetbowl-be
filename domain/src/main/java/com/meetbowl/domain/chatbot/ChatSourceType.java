package com.meetbowl.domain.chatbot;

/** AI 서버 계약과 동일한 값만 허용해 권한 필터가 정의되지 않은 자료 유형의 검색을 막는다. */
public enum ChatSourceType {
    BACKUP_MAIL,
    MINUTES,
    PERSONAL_MEMO,
    PERSONAL_DRIVE_FILE,
    SHARED_WORKSPACE_FILE_VERSION
}
