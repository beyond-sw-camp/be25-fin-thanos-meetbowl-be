CREATE TABLE mail (
    id BINARY(16) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    sender_user_id BINARY(16) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    body MEDIUMTEXT NOT NULL,
    mail_type VARCHAR(30) NOT NULL,
    body_type VARCHAR(30) NOT NULL,
    related_resource_type VARCHAR(40) NULL,
    related_resource_id BINARY(16) NULL,
    idempotency_key BINARY(16) NOT NULL,
    delivery_status VARCHAR(20) NOT NULL,
    requested_at DATETIME(6) NULL,
    sent_at DATETIME(6) NULL,
    failed_at DATETIME(6) NULL,
    failure_code VARCHAR(100) NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_mail_idempotency_key UNIQUE (idempotency_key),
    INDEX idx_mail_organization_requested_at (organization_id, requested_at),
    INDEX idx_mail_sender_requested_at (sender_user_id, requested_at),
    INDEX idx_mail_delivery_status (delivery_status)
);

CREATE TABLE mail_recipient (
    mail_id BINARY(16) NOT NULL,
    recipient_user_id BINARY(16) NOT NULL,
    recipient_order INT NOT NULL,
    CONSTRAINT uk_mail_recipient_mail_user UNIQUE (mail_id, recipient_user_id),
    CONSTRAINT fk_mail_recipient_mail FOREIGN KEY (mail_id) REFERENCES mail (id),
    INDEX idx_mail_recipient_user (recipient_user_id)
);

CREATE TABLE mailbox_entry (
    id BINARY(16) NOT NULL,
    mail_id BINARY(16) NOT NULL,
    owner_user_id BINARY(16) NOT NULL,
    mailbox_type VARCHAR(20) NOT NULL,
    read_at DATETIME(6) NULL,
    trashed_at DATETIME(6) NULL,
    permanently_deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_mailbox_entry_mail_owner_type UNIQUE (mail_id, owner_user_id, mailbox_type),
    CONSTRAINT fk_mailbox_entry_mail FOREIGN KEY (mail_id) REFERENCES mail (id),
    INDEX idx_mailbox_entry_owner_type (owner_user_id, mailbox_type),
    INDEX idx_mailbox_entry_owner_trashed (owner_user_id, trashed_at)
);

CREATE TABLE mail_attachment (
    id BINARY(16) NOT NULL,
    mail_id BINARY(16) NOT NULL,
    uploader_user_id BINARY(16) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_mail_attachment_object_key UNIQUE (object_key),
    CONSTRAINT fk_mail_attachment_mail FOREIGN KEY (mail_id) REFERENCES mail (id),
    INDEX idx_mail_attachment_mail (mail_id)
);
