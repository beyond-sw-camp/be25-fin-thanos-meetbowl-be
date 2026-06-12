package com.meetbowl.domain.mail;

/** 메일 발송 생명주기 상태다. 작성(DRAFT)→요청(REQUESTED)→완료(SENT) 흐름이며, 실패(FAILED)는 재요청(RETRYING)으로만 다시 진행한다. */
public enum MailDeliveryStatus {
    DRAFT,
    REQUESTED,
    RETRYING,
    SENT,
    FAILED
}
