package com.meetbowl.infrastructure.persistence.mail;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import com.meetbowl.domain.mail.ExternalMailRecipient;

/** 외부 메일 수신자 이름/이메일을 mail aggregate에 함께 저장하는 값 객체다. */
@Embeddable
public class ExternalMailRecipientValue {

    @Column(name = "recipient_name", nullable = false, length = 120)
    private String name;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String email;

    protected ExternalMailRecipientValue() {}

    private ExternalMailRecipientValue(String name, String email) {
        this.name = name;
        this.email = email;
    }

    static ExternalMailRecipientValue from(ExternalMailRecipient recipient) {
        return new ExternalMailRecipientValue(recipient.name(), recipient.email());
    }

    ExternalMailRecipient toDomain() {
        return new ExternalMailRecipient(name, email);
    }
}
