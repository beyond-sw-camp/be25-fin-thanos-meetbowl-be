package com.meetbowl.domain.mail;

/** 메일의 발신 성격이다. 사용자 발송(NORMAL), 관리자 공지(ANNOUNCEMENT), 시스템 내부 발송(SYSTEM)을 구분한다. */
public enum MailType {
    NORMAL,
    ANNOUNCEMENT,
    SYSTEM
}
