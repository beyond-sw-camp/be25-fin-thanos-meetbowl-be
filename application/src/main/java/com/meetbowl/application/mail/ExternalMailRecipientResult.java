package com.meetbowl.application.mail;

import com.meetbowl.domain.mail.ExternalMailRecipient;

/** 메일 응답에 노출하는 외부 수신자 정보다. */
public record ExternalMailRecipientResult(String name, String email) {

    public static ExternalMailRecipientResult from(ExternalMailRecipient recipient) {
        return new ExternalMailRecipientResult(recipient.name(), recipient.email());
    }
}
