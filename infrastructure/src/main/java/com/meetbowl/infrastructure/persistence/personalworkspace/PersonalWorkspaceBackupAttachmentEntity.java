package com.meetbowl.infrastructure.persistence.personalworkspace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupAttachment;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 개인 워크스페이스 백업에 귀속된 첨부파일 스냅샷을 저장하는 엔티티다.
 *
 * <p>원본 메일/첨부({@code mail_attachment})는 보존 정책으로 삭제되므로, 백업이 그와 독립적으로 첨부 메타데이터를 보관하도록 별도 테이블로 둔다.
 * {@code object_key}는 파일 실체(S3 객체)를 가리키며, 같은 원본을 여러 유저가 백업할 수 있어 유니크 제약은 두지 않는다.
 */
@Entity
@Table(
        name = "personal_workspace_backup_attachments",
        indexes = @Index(name = "idx_pw_backup_attachment_backup", columnList = "backup_id"))
public class PersonalWorkspaceBackupAttachmentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "backup_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pw_backup_attachment_backup"))
    private PersonalWorkspaceBackupEntity backup;

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

    protected PersonalWorkspaceBackupAttachmentEntity() {}

    static PersonalWorkspaceBackupAttachmentEntity from(
            PersonalWorkspaceBackupEntity backup, PersonalWorkspaceBackupAttachment attachment) {
        PersonalWorkspaceBackupAttachmentEntity entity =
                new PersonalWorkspaceBackupAttachmentEntity();
        entity.backup = backup;
        entity.objectKey = attachment.objectKey();
        entity.originalFileName = attachment.originalFileName();
        entity.storedFileName = attachment.storedFileName();
        entity.mimeType = attachment.mimeType();
        entity.sizeBytes = attachment.sizeBytes();
        return entity;
    }

    PersonalWorkspaceBackupAttachment toDomain() {
        return PersonalWorkspaceBackupAttachment.of(
                objectKey, originalFileName, storedFileName, mimeType, sizeBytes);
    }
}
