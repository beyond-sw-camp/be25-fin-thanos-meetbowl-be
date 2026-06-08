package com.meetbowl.domain.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public class PersonalWorkspaceCalendarEvent {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final int MAX_EXTERNAL_EVENT_ID_LENGTH = 255;

    private final UUID id;
    private final UUID ownerUserId;
    private final String title;
    private final String description;
    private final Instant startedAt;
    private final Instant endedAt;
    private final boolean allDay;
    private final CalendarEventSource source;
    private final UUID sourceId;
    private final String externalEventId;

    private PersonalWorkspaceCalendarEvent(
            UUID id,
            UUID ownerUserId,
            String title,
            String description,
            Instant startedAt,
            Instant endedAt,
            boolean allDay,
            CalendarEventSource source,
            UUID sourceId,
            String externalEventId) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.description = description;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.allDay = allDay;
        this.source = source;
        this.sourceId = sourceId;
        this.externalEventId = externalEventId;
    }

    public static PersonalWorkspaceCalendarEvent createPersonal(
            UUID ownerUserId,
            String title,
            String description,
            Instant startedAt,
            Instant endedAt,
            boolean allDay) {
        return of(
                null,
                ownerUserId,
                title,
                description,
                startedAt,
                endedAt,
                allDay,
                CalendarEventSource.PERSONAL,
                null,
                null);
    }

    public static PersonalWorkspaceCalendarEvent of(
            UUID id,
            UUID ownerUserId,
            String title,
            String description,
            Instant startedAt,
            Instant endedAt,
            boolean allDay,
            CalendarEventSource source,
            UUID sourceId,
            String externalEventId) {
        requireId(ownerUserId, "일정 소유자 ID는 필수입니다.");
        requireText(title, MAX_TITLE_LENGTH, "일정 제목은 필수입니다.", "일정 제목은 100자 이하여야 합니다.");
        requireRange(startedAt, endedAt);
        if (source == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "일정 출처는 필수입니다.");
        }
        if (source == CalendarEventSource.MEETING && sourceId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의 일정은 출처 ID가 필수입니다.");
        }
        validateOptionalLength(description, MAX_DESCRIPTION_LENGTH, "일정 설명은 1000자 이하여야 합니다.");
        validateOptionalLength(
                externalEventId, MAX_EXTERNAL_EVENT_ID_LENGTH, "외부 일정 ID는 255자 이하여야 합니다.");

        return new PersonalWorkspaceCalendarEvent(
                id,
                ownerUserId,
                title.trim(),
                normalize(description),
                startedAt,
                endedAt,
                allDay,
                source,
                sourceId,
                normalize(externalEventId));
    }

    public PersonalWorkspaceCalendarEvent update(
            String title, String description, Instant startedAt, Instant endedAt, boolean allDay) {
        return of(
                id,
                ownerUserId,
                title,
                description,
                startedAt,
                endedAt,
                allDay,
                source,
                sourceId,
                externalEventId);
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

    public String description() {
        return description;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant endedAt() {
        return endedAt;
    }

    public boolean allDay() {
        return allDay;
    }

    public CalendarEventSource source() {
        return source;
    }

    public UUID sourceId() {
        return sourceId;
    }

    public String externalEventId() {
        return externalEventId;
    }

    private static void requireRange(Instant startedAt, Instant endedAt) {
        if (startedAt == null || endedAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "일정 시작/종료 시간은 필수입니다.");
        }
        if (!startedAt.isBefore(endedAt)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "일정 시작 시간은 종료 시간보다 이전이어야 합니다.");
        }
    }

    static void requireId(UUID id, String message) {
        if (id == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
        }
    }

    static void requireText(
            String value, int maxLength, String requiredMessage, String maxLengthMessage) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, requiredMessage);
        }
        if (value.trim().length() > maxLength) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, maxLengthMessage);
        }
    }

    static void validateOptionalLength(String value, int maxLength, String message) {
        String normalized = normalize(value);
        if (normalized != null && normalized.length() > maxLength) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
        }
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
