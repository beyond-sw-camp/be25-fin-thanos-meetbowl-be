package com.meetbowl.domain.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class MeetingTest {

    private static final Instant SCHEDULED_AT = Instant.parse("2099-01-01T01:00:00Z");
    private static final Instant SCHEDULED_END_AT = Instant.parse("2099-01-01T02:00:00Z");

    private Meeting scheduledMeeting() {
        return Meeting.create(
                "원본 회의",
                SCHEDULED_AT,
                SCHEDULED_END_AT,
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                "원본 내용");
    }

    @Test
    void changeUpdatesEditableFieldsAndPreservesTheRest() {
        Meeting original = scheduledMeeting();
        Instant newScheduledAt = Instant.parse("2099-02-02T02:00:00Z");
        Instant newScheduledEndAt = Instant.parse("2099-02-02T03:00:00Z");
        UUID newRoomId = UUID.randomUUID();

        Meeting changed =
                original.change("변경된 회의", newScheduledAt, newScheduledEndAt, newRoomId, "변경된 내용");

        // 수정된 필드
        assertEquals("변경된 회의", changed.title());
        assertEquals("변경된 내용", changed.description());
        assertEquals(newScheduledAt, changed.scheduledAt());
        assertEquals(newScheduledEndAt, changed.scheduledEndAt());
        assertEquals(newRoomId, changed.meetingRoomId());
        // 보존된 필드
        assertEquals(original.id(), changed.id());
        assertEquals(original.hostUserId(), changed.hostUserId());
        assertEquals(MeetingStatus.SCHEDULED, changed.status());
        assertEquals(original.startedAt(), changed.startedAt());
        assertEquals(original.endedAt(), changed.endedAt());
    }

    @Test
    void changeAllowsClearingMeetingRoom() {
        Meeting original = scheduledMeeting();

        Meeting changed =
                original.change(
                        original.title(),
                        original.scheduledAt(),
                        original.scheduledEndAt(),
                        null,
                        original.description());

        assertEquals(null, changed.meetingRoomId());
    }

    @Test
    void changeAllowedWhileInProgress() {
        Meeting inProgress = scheduledMeeting().start(Instant.parse("2099-01-01T01:05:00Z"));

        Meeting changed =
                inProgress.change(
                        "진행 중 수정", SCHEDULED_AT, SCHEDULED_END_AT, UUID.randomUUID(), "내용");

        assertEquals("진행 중 수정", changed.title());
        assertEquals(MeetingStatus.IN_PROGRESS, changed.status());
    }

    @Test
    void changeRejectedWhenEnded() {
        Meeting ended =
                scheduledMeeting()
                        .start(Instant.parse("2099-01-01T01:05:00Z"))
                        .end(Instant.parse("2099-01-01T02:00:00Z"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                ended.change(
                                        "수정 시도",
                                        SCHEDULED_AT,
                                        SCHEDULED_END_AT,
                                        UUID.randomUUID(),
                                        "내용"));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void changeRejectedWhenCancelled() {
        Meeting cancelled = scheduledMeeting().cancel();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                cancelled.change(
                                        "수정 시도",
                                        SCHEDULED_AT,
                                        SCHEDULED_END_AT,
                                        UUID.randomUUID(),
                                        "내용"));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void changeRejectsBlankTitle() {
        Meeting original = scheduledMeeting();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                original.change(
                                        " ",
                                        SCHEDULED_AT,
                                        SCHEDULED_END_AT,
                                        UUID.randomUUID(),
                                        "내용"));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void createRejectsNullScheduledEndAt() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                Meeting.create(
                                        "회의",
                                        SCHEDULED_AT,
                                        null,
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        null,
                                        null,
                                        null));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void createRejectsScheduledEndBeforeOrEqualStart() {
        // 종료가 시작과 같거나 이전이면 거부한다(겹침 판정의 유효 구간을 보장).
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                Meeting.create(
                                        "회의",
                                        SCHEDULED_AT,
                                        SCHEDULED_AT,
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        null,
                                        null,
                                        null));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }
}
