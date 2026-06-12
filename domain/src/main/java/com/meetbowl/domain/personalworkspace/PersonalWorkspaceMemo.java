package com.meetbowl.domain.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 사용자 개인 메모 애그리거트다.
 *
 * <p>제목·내용 길이와 소유자, 생성/수정 시각 정합성(수정이 생성보다 빠를 수 없음)을 생성 시점에 검증한다. 수정은 새 인스턴스를 만들어 불변성을 유지한다.
 */
public class PersonalWorkspaceMemo {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_CONTENT_LENGTH = 10000;

    private final UUID id;
    private final UUID ownerUserId;
    private final String title;
    private final String content;
    private final Instant memoCreatedAt;
    private final Instant memoUpdatedAt;

    private PersonalWorkspaceMemo(
            UUID id,
            UUID ownerUserId,
            String title,
            String content,
            Instant memoCreatedAt,
            Instant memoUpdatedAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.content = content;
        this.memoCreatedAt = memoCreatedAt;
        this.memoUpdatedAt = memoUpdatedAt;
    }

    public static PersonalWorkspaceMemo create(
            UUID ownerUserId, String title, String content, Instant memoCreatedAt) {
        return of(null, ownerUserId, title, content, memoCreatedAt, memoCreatedAt);
    }

    public static PersonalWorkspaceMemo of(
            UUID id,
            UUID ownerUserId,
            String title,
            String content,
            Instant memoCreatedAt,
            Instant memoUpdatedAt) {
        PersonalWorkspaceCalendarEvent.requireId(ownerUserId, "메모 소유자 ID는 필수입니다.");
        PersonalWorkspaceCalendarEvent.requireText(
                title, MAX_TITLE_LENGTH, "메모 제목은 필수입니다.", "메모 제목은 100자 이하여야 합니다.");
        PersonalWorkspaceCalendarEvent.requireText(
                content, MAX_CONTENT_LENGTH, "메모 내용은 필수입니다.", "메모 내용은 10000자 이하여야 합니다.");
        if (memoCreatedAt == null || memoUpdatedAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "메모 생성/수정 시각은 필수입니다.");
        }
        if (memoUpdatedAt.isBefore(memoCreatedAt)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "메모 수정 시각은 생성 시각보다 이전일 수 없습니다.");
        }

        return new PersonalWorkspaceMemo(
                id, ownerUserId, title.trim(), content.trim(), memoCreatedAt, memoUpdatedAt);
    }

    public PersonalWorkspaceMemo update(String title, String content, Instant memoUpdatedAt) {
        return of(id, ownerUserId, title, content, memoCreatedAt, memoUpdatedAt);
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

    public String title() {
        return title;
    }

    public String content() {
        return content;
    }

    public Instant memoCreatedAt() {
        return memoCreatedAt;
    }

    public Instant memoUpdatedAt() {
        return memoUpdatedAt;
    }
}
