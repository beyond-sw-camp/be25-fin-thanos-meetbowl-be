package com.meetbowl.domain.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public class PersonalWorkspaceCalendarEvent {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    private final UUID id;
    private final UUID ownerUserId;
    private final String title;
    private final String description;
    private final Instant startedAt;
    private final Instant endedAt;
    private final boolean allDay;
    private final CalendarEventSource source;
    private final UUID sourceId;

    private PersonalWorkspaceCalendarEvent(
            UUID id,
            UUID ownerUserId,
            String title,
            String description,
            Instant startedAt,
            Instant endedAt,
            boolean allDay,
            CalendarEventSource source,
            UUID sourceId) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.description = description;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.allDay = allDay;
        this.source = source;
        this.sourceId = sourceId;
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
                null);
    }

    /**
     * 회의 도메인의 일정을 사용자 개인 캘린더에 투영한다.
     *
     * <p>회의가 일정의 기준 데이터이므로 개인 캘린더는 회의 ID를 출처로 보존해야 한다. 이 연결이 있어야 회의 수정·취소 시 별도의 일정 ID를 전달받지 않고도 관련된
     * 사용자 일정을 일관되게 갱신할 수 있다.
     */
    public static PersonalWorkspaceCalendarEvent createFromMeeting(
            UUID ownerUserId,
            UUID meetingId,
            String title,
            String description,
            Instant startedAt,
            Instant endedAt) {
        return of(
                null,
                ownerUserId,
                title,
                description,
                startedAt,
                endedAt,
                false,
                CalendarEventSource.MEETING,
                meetingId);
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
            UUID sourceId) {
        requireId(ownerUserId, "일정 소유자 ID는 필수입니다.");
        requireText(title, MAX_TITLE_LENGTH, "일정 제목은 필수입니다.", "일정 제목은 100자 이하여야 합니다.");
        requireRange(startedAt, endedAt);
        if (source == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "일정 출처는 필수입니다.");
        }
        validateSourceReference(source, sourceId);
        validateOptionalLength(description, MAX_DESCRIPTION_LENGTH, "일정 설명은 1000자 이하여야 합니다.");

        return new PersonalWorkspaceCalendarEvent(
                id,
                ownerUserId,
                title.trim(),
                normalize(description),
                startedAt,
                endedAt,
                allDay,
                source,
                sourceId);
    }

    private static void validateSourceReference(CalendarEventSource source, UUID sourceId) {
        switch (source) {
            case PERSONAL -> {
                if (sourceId != null) {
                    throw new BusinessException(
                            ErrorCode.COMMON_INVALID_REQUEST, "개인 일정에는 출처 ID를 지정할 수 없습니다.");
                }
            }
            case MEETING -> {
                if (sourceId == null) {
                    throw new BusinessException(
                            ErrorCode.COMMON_INVALID_REQUEST, "회의 일정은 출처 ID가 필수입니다.");
                }
            }
        }
    }

    /**
     * 사용자가 직접 만든 개인 일정만 수정한다.
     *
     * <p>회의 일정은 회의 도메인의 복제 데이터이므로 캘린더에서 직접 바꾸면 회의 정보와 불일치한다. 회의 일정 변경은 반드시 회의 수정 흐름에서 {@link
     * #syncFromMeeting(String, String, Instant, Instant)}을 통해 반영한다.
     */
    public PersonalWorkspaceCalendarEvent updatePersonal(
            String title, String description, Instant startedAt, Instant endedAt, boolean allDay) {
        if (source != CalendarEventSource.PERSONAL) {
            throw new BusinessException(
                    ErrorCode.COMMON_FORBIDDEN, "회의에서 생성된 일정은 개인 캘린더에서 직접 수정할 수 없습니다.");
        }
        return of(
                id, ownerUserId, title, description, startedAt, endedAt, allDay, source, sourceId);
    }

    /**
     * 회의 수정 결과를 기존 사용자 일정에 반영한다.
     *
     * <p>이 메서드는 회의 Application 흐름만 사용해야 한다. 출처와 소유자를 그대로 유지해 동일 회의 일정의 식별 관계가 변경되지 않도록 한다.
     */
    public PersonalWorkspaceCalendarEvent syncFromMeeting(
            String title, String description, Instant startedAt, Instant endedAt) {
        if (source != CalendarEventSource.MEETING) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "개인 일정은 회의 동기화 대상으로 사용할 수 없습니다.");
        }
        return of(id, ownerUserId, title, description, startedAt, endedAt, false, source, sourceId);
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
