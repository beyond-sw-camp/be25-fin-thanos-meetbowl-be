package com.meetbowl.domain.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public class PersonalWorkspaceBackup {

    private static final int MAX_TITLE_LENGTH = 150;
    private static final int MAX_SUMMARY_LENGTH = 1000;

    private final UUID id;
    private final UUID ownerUserId;
    private final PersonalWorkspaceBackupSourceType sourceType;
    private final UUID sourceId;
    private final String title;
    private final String summary;
    private final Instant backedUpAt;

    private PersonalWorkspaceBackup(
            UUID id,
            UUID ownerUserId,
            PersonalWorkspaceBackupSourceType sourceType,
            UUID sourceId,
            String title,
            String summary,
            Instant backedUpAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.title = title;
        this.summary = summary;
        this.backedUpAt = backedUpAt;
    }

    public static PersonalWorkspaceBackup create(
            UUID ownerUserId,
            PersonalWorkspaceBackupSourceType sourceType,
            UUID sourceId,
            String title,
            String summary,
            Instant backedUpAt) {
        return of(null, ownerUserId, sourceType, sourceId, title, summary, backedUpAt);
    }

    public static PersonalWorkspaceBackup of(
            UUID id,
            UUID ownerUserId,
            PersonalWorkspaceBackupSourceType sourceType,
            UUID sourceId,
            String title,
            String summary,
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

    public Instant backedUpAt() {
        return backedUpAt;
    }
}
