package com.meetbowl.infrastructure.persistence.mail;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.mail.MailAttachment;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 메일에 귀속된 첨부파일 메타데이터를 저장하는 infrastructure 전용 엔티티다.
 *
 * <p>첨부파일의 생명주기는 메일 애그리거트가 소유하므로 {@link MailEntity}와 함께 저장·삭제되며, 파일 원본은 Object Storage에 남겨 DB와 대용량
 * 바이너리 저장 책임을 분리한다.
 */
@Entity
@Table(
        name = "mail_attachment",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_mail_attachment_object_key",
                        columnNames = "object_key"),
        indexes = @Index(name = "idx_mail_attachment_mail", columnList = "mail_id"))
public class MailAttachmentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "mail_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_mail_attachment_mail"))
    private MailEntity mail;

    @Column(name = "uploader_user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID uploaderUserId;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    @Column(name = "mime_type", nullable = false, length = 150)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    protected MailAttachmentEntity() {}

    static MailAttachmentEntity from(MailEntity mail, MailAttachment attachment) {
        MailAttachmentEntity entity = new MailAttachmentEntity();
        entity.setId(attachment.id());
        entity.mail = mail;
        entity.uploaderUserId = attachment.uploaderUserId();
        entity.objectKey = attachment.objectKey();
        entity.originalFileName = attachment.originalFileName();
        entity.storedFileName = attachment.storedFileName();
        entity.mimeType = attachment.mimeType();
        entity.sizeBytes = attachment.sizeBytes();
        return entity;
    }

    MailAttachment toDomain() {
        return MailAttachment.of(
                getId(),
                uploaderUserId,
                objectKey,
                originalFileName,
                storedFileName,
                mimeType,
                sizeBytes);
    }
}
