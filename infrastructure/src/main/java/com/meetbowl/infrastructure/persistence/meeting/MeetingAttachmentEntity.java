package com.meetbowl.infrastructure.persistence.meeting;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.meetbowl.domain.meeting.MeetingAttachment;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 회의 첨부파일 메타데이터 JPA Entity다. {@code meeting_attachment} 테이블과 1:1로 매핑된다. 원본 파일은 Object Storage에 두고
 * DB에는 {@code objectKey} 등 메타데이터만 저장한다(원본 저장 금지).
 */
@Entity
@Table(
        name = "meeting_attachments",
        indexes = {@Index(name = "idx_meeting_attachment_meeting", columnList = "meeting_id")})
public class MeetingAttachmentEntity extends BaseEntity {

    /** 소속 회의(FK). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID meetingId;

    /** 업로드한 사용자(FK). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID uploadedByUserId;

    /** Object Storage 객체 키(원본 위치). */
    @Column(nullable = false, length = 512)
    private String objectKey;

    /** 사용자가 올린 원본 파일명. */
    @Column(nullable = false, length = 255)
    private String originalFileName;

    /** MIME 타입(nullable). */
    @Column(length = 150)
    private String mimeType;

    /** 파일 크기(바이트). */
    @Column(nullable = false)
    private long sizeBytes;

    protected MeetingAttachmentEntity() {}

    private MeetingAttachmentEntity(
            UUID meetingId,
            UUID uploadedByUserId,
            String objectKey,
            String originalFileName,
            String mimeType,
            long sizeBytes) {
        this.meetingId = meetingId;
        this.uploadedByUserId = uploadedByUserId;
        this.objectKey = objectKey;
        this.originalFileName = originalFileName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
    }

    static MeetingAttachmentEntity from(MeetingAttachment attachment) {
        MeetingAttachmentEntity entity =
                new MeetingAttachmentEntity(
                        attachment.meetingId(),
                        attachment.uploadedByUserId(),
                        attachment.objectKey(),
                        attachment.originalFileName(),
                        attachment.mimeType(),
                        attachment.sizeBytes());
        entity.setId(attachment.id());
        return entity;
    }

    MeetingAttachment toDomain() {
        return MeetingAttachment.of(
                getId(),
                meetingId,
                uploadedByUserId,
                objectKey,
                originalFileName,
                mimeType,
                sizeBytes);
    }
}
