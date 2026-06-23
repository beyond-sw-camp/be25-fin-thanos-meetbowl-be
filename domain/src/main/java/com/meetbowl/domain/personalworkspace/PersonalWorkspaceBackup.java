package com.meetbowl.domain.personalworkspace;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 개인 워크스페이스 백업 자료 애그리거트다.
 *
 * <p>메일·회의록 등 원본 자료를 개인 보관함으로 옮긴 사본을 표현한다. 출처 유형/ID로 원본을 가리키며, 소유자·제목·백업 시각을 필수로 검증한다.
 *
 * <p>원본이 보존 정책으로 삭제돼도 사본만으로 내용을 볼 수 있도록, 목록용 요약(summary)과 별개로 본문 전체(body)와 첨부 메타데이터(attachments)를
 * 함께 보관한다.
 */
public class PersonalWorkspaceBackup {

    private static final int MAX_TITLE_LENGTH = 150;
    private static final int MAX_SUMMARY_LENGTH = 1000;

    private final UUID id;
    private final UUID ownerUserId;
    private final PersonalWorkspaceBackupSourceType sourceType;
    private final UUID sourceId;
    private final String title;
    private final String summary;
    private final String body;
    private final List<PersonalWorkspaceBackupAttachment> attachments;
    private final Instant backedUpAt;

    private PersonalWorkspaceBackup(
            UUID id,
            UUID ownerUserId,
            PersonalWorkspaceBackupSourceType sourceType,
            UUID sourceId,
            String title,
            String summary,
            String body,
            List<PersonalWorkspaceBackupAttachment> attachments,
            Instant backedUpAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.title = title;
        this.summary = summary;
        this.body = body;
        this.attachments = attachments;
        this.backedUpAt = backedUpAt;
    }

    public static PersonalWorkspaceBackup create(
            UUID ownerUserId,
            PersonalWorkspaceBackupSourceType sourceType,
            UUID sourceId,
            String title,
            String summary,
            Instant backedUpAt) {
        return create(
                ownerUserId, sourceType, sourceId, title, summary, null, List.of(), backedUpAt);
    }

    public static PersonalWorkspaceBackup create(
            UUID ownerUserId,
            PersonalWorkspaceBackupSourceType sourceType,
            UUID sourceId,
            String title,
            String summary,
            String body,
            List<PersonalWorkspaceBackupAttachment> attachments,
            Instant backedUpAt) {
        return of(
                null,
                ownerUserId,
                sourceType,
                sourceId,
                title,
                summary,
                body,
                attachments,
                backedUpAt);
    }

    public static PersonalWorkspaceBackup of(
            UUID id,
            UUID ownerUserId,
            PersonalWorkspaceBackupSourceType sourceType,
            UUID sourceId,
            String title,
            String summary,
            Instant backedUpAt) {
        return of(
                id, ownerUserId, sourceType, sourceId, title, summary, null, List.of(), backedUpAt);
    }

    public static PersonalWorkspaceBackup of(
            UUID id,
            UUID ownerUserId,
            PersonalWorkspaceBackupSourceType sourceType,
            UUID sourceId,
            String title,
            String summary,
            String body,
            List<PersonalWorkspaceBackupAttachment> attachments,
            Instant backedUpAt) {
        PersonalWorkspaceCalendarEvent.requireId(ownerUserId, "백업 소유자 ID는 필수입니다.");
        PersonalWorkspaceCalendarEvent.requireId(sourceId, "백업 원본 ID는 필수입니다.");
        if (sourceType == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "백업 원본 유형은 필수입니다.");
        }
        PersonalWorkspaceCalendarEvent.requireText(
                title, MAX_TITLE_LENGTH, "백업 제목은 필수입니다.", "백업 제목은 150자 이하여야 합니다.");
        PersonalWorkspaceCalendarEvent.validateOptionalLength(
                summary, MAX_SUMMARY_LENGTH, "백업 요약은 1000자 이하여야 합니다.");
        if (backedUpAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "백업 시각은 필수입니다.");
        }

        return new PersonalWorkspaceBackup(
                id,
                ownerUserId,
                sourceType,
                sourceId,
                title.trim(),
                PersonalWorkspaceCalendarEvent.normalize(summary),
                PersonalWorkspaceCalendarEvent.normalize(body),
                attachments == null ? List.of() : List.copyOf(attachments),
                backedUpAt);
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerUserId.equals(userId);
    }

    public UUID id() {
        return id;
    }

    public UUID ownerUserId() {
        return ownerUserId;
    }

    public PersonalWorkspaceBackupSourceType sourceType() {
        return sourceType;
    }

    public UUID sourceId() {
        return sourceId;
    }

    public String title() {
        return title;
    }

    public String summary() {
        return summary;
    }

    public String body() {
        return body;
    }

    public List<PersonalWorkspaceBackupAttachment> attachments() {
        return attachments;
    }

    public Instant backedUpAt() {
        return backedUpAt;
    }
}
