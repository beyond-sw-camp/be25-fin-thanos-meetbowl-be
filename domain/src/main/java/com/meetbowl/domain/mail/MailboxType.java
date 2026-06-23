package com.meetbowl.domain.mail;

/** 메일함 항목이 받은함(INBOX)인지 보낸함(SENT)인지를 구분한다. 한 메일에 발신자 SENT·수신자 INBOX 항목이 각각 생긴다. */
public enum MailboxType {
    INBOX,
    SENT
}
