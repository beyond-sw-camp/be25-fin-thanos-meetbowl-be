package com.meetbowl.domain.mail;

/** 메일 본문의 형식이다. 일반 텍스트(TEXT)와 회의록 공유(MINUTES_SHARE)를 구분해 화면 표현과 관련 리소스 연결을 다르게 다룬다. */
public enum MailBodyType {
    TEXT,
    MINUTES_SHARE
}
