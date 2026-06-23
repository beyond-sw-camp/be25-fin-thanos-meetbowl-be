package com.meetbowl.domain.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** 사용자가 백업 자료에 표시한 북마크 애그리거트다. (소유자, 백업) 단위로 존재하며 소유자·백업 ID·북마크 시각을 필수로 검증한다. */
public class PersonalWorkspaceBackupBookmark {

    private final UUID id;
    private final UUID ownerUserId;
    private final UUID backupId;
    private final Instant bookmarkedAt;

    private PersonalWorkspaceBackupBookmark(
            UUID id, UUID ownerUserId, UUID backupId, Instant bookmarkedAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.backupId = backupId;
        this.bookmarkedAt = bookmarkedAt;
    }

    public static PersonalWorkspaceBackupBookmark create(
            UUID ownerUserId, UUID backupId, Instant bookmarkedAt) {
        return of(null, ownerUserId, backupId, bookmarkedAt);
    }

    public static PersonalWorkspaceBackupBookmark of(
            UUID id, UUID ownerUserId, UUID backupId, Instant bookmarkedAt) {
        PersonalWorkspaceCalendarEvent.requireId(ownerUserId, "북마크 소유자 ID는 필수입니다.");
        PersonalWorkspaceCalendarEvent.requireId(backupId, "백업 ID는 필수입니다.");
        if (bookmarkedAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "북마크 시각은 필수입니다.");
        }

        return new PersonalWorkspaceBackupBookmark(id, ownerUserId, backupId, bookmarkedAt);
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

    public UUID backupId() {
        return backupId;
    }

    public Instant bookmarkedAt() {
        return bookmarkedAt;
    }
}
