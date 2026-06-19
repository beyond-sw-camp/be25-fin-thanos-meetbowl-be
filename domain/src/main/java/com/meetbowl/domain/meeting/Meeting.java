package com.meetbowl.domain.meeting;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 회의 본체 도메인 모델이다.
 *
 * <p>"새 회의 예약" 폼이 만드는 회의 한 건을 표현한다. 회의가 사용하는 회의실은 {@code meetingRoomId} raw UUID로 직접 참조하며, 회의실
 * 상세(이름/건물/수용 인원)는 조회 시 회의실 기준정보를 조인해 가져온다. 화상회의만 진행하는 경우 {@code meetingRoomId}는 null이다. 이 모델은 회의의
 * 주최자·회의실 연결·화상 provider 연결·진행 상태를 소유한다. 참석자는 별도 도메인 모델({@link MeetingAttendee})이 회의를 {@code
 * meetingId}로 참조해 소유한다.
 *
 * <p>예약 시점에 정하는 일정은 {@code scheduledAt}(예정 시작)과 {@code scheduledEndAt}(예정 종료)이며, 회의실 중복 예약(시간대 겹침)
 * 판정은 이 두 값으로 한다. {@code startedAt}/{@code endedAt}은 회의가 실제로 진행될 때만 채워지는 기록이라 겹침 판정에는 쓰지 않는다.
 *
 * <p>불변 객체로 다루며 상태 전환은 새 인스턴스를 반환한다. 다른 도메인(사용자/회의실)은 raw UUID 참조로만 연결한다.
 */
public class Meeting {

    private final UUID id;

    /** 회의 제목(필수). */
    private final String title;

    /** 예정 시작 시각(필수). 실제 시작 전까지 기준이 되는 일정 시작 시각. */
    private final Instant scheduledAt;

    /** 예정 종료 시각(필수). 회의실 시간대 겹침 판정의 종료 경계로 쓴다. {@code scheduledAt}보다 이후여야 한다. */
    private final Instant scheduledEndAt;

    /** 주최자 사용자(FK). 회의 생성 요청자이며 참석자 중 HOST 역할. */
    private final UUID hostUserId;

    /** 사용 회의실(FK, nullable). 화상회의만 진행하면 null. 상세는 회의실 기준정보를 조인해 조회한다. */
    private final UUID meetingRoomId;

    /** 화상회의 provider 식별자(예: LIVEKIT). 외부 연동 전이면 null. */
    private final String provider;

    /** provider 측 회의방 식별자. 외부 연동 전이면 null. */
    private final String providerRoomId;

    /** 회의 상태(SCHEDULED/IN_PROGRESS/ENDED/CANCELLED). */
    private final MeetingStatus status;

    /** 실제 시작 시각(UTC, nullable). 아직 시작 전이면 null. */
    private final Instant startedAt;

    /** 실제 종료 시각(UTC, nullable). 아직 종료 전이면 null. */
    private final Instant endedAt;

    /** 회의 내용/안건(선택, nullable). 회의실 예약 폼의 "회의 내용"에 입력되며 회의 상세 조회에서 노출한다. */
    private final String description;

    private Meeting(
            UUID id,
            String title,
            Instant scheduledAt,
            Instant scheduledEndAt,
            UUID hostUserId,
            UUID meetingRoomId,
            String provider,
            String providerRoomId,
            MeetingStatus status,
            Instant startedAt,
            Instant endedAt,
            String description) {
        this.id = id;
        this.title = title;
        this.scheduledAt = scheduledAt;
        this.scheduledEndAt = scheduledEndAt;
        this.hostUserId = hostUserId;
        this.meetingRoomId = meetingRoomId;
        this.provider = provider;
        this.providerRoomId = providerRoomId;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.description = description;
    }

    public static Meeting create(
            String title,
            Instant scheduledAt,
            Instant scheduledEndAt,
            UUID hostUserId,
            UUID meetingRoomId,
            String provider,
            String providerRoomId,
            String description) {
        return of(
                null,
                title,
                scheduledAt,
                scheduledEndAt,
                hostUserId,
                meetingRoomId,
                provider,
                providerRoomId,
                MeetingStatus.SCHEDULED,
                null,
                null,
                description);
    }

    public static Meeting of(
            UUID id,
            String title,
            Instant scheduledAt,
            Instant scheduledEndAt,
            UUID hostUserId,
            UUID meetingRoomId,
            String provider,
            String providerRoomId,
            MeetingStatus status,
            Instant startedAt,
            Instant endedAt,
            String description) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의 제목은 필수입니다.");
        }
        if (scheduledAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의 예정 시작 시각은 필수입니다.");
        }
        if (scheduledEndAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의 예정 종료 시각은 필수입니다.");
        }
        if (!scheduledAt.isBefore(scheduledEndAt)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "회의 예정 시작 시각은 예정 종료 시각보다 이전이어야 합니다.");
        }
        if (hostUserId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의 주최자는 필수입니다.");
        }
        if (status == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의 상태는 필수입니다.");
        }
        if (startedAt != null && endedAt != null && !startedAt.isBefore(endedAt)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "회의 시작 시각은 종료 시각보다 이전이어야 합니다.");
        }
        return new Meeting(
                id,
                title,
                scheduledAt,
                scheduledEndAt,
                hostUserId,
                meetingRoomId,
                provider,
                providerRoomId,
                status,
                startedAt,
                endedAt,
                description);
    }

    /** 회의 시작(예정 → 진행). 이미 진행/종료/취소된 회의는 시작할 수 없다. */
    public Meeting start(Instant startedAt) {
        if (status != MeetingStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "예정 상태의 회의만 시작할 수 있습니다.");
        }
        return of(
                id,
                title,
                scheduledAt,
                scheduledEndAt,
                hostUserId,
                meetingRoomId,
                provider,
                providerRoomId,
                MeetingStatus.IN_PROGRESS,
                startedAt,
                endedAt,
                description);
    }

    /** 회의 종료(진행 → 종료). 진행 중인 회의만 종료할 수 있다. */
    public Meeting end(Instant endedAt) {
        if (status != MeetingStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "진행 중인 회의만 종료할 수 있습니다.");
        }
        return of(
                id,
                title,
                scheduledAt,
                scheduledEndAt,
                hostUserId,
                meetingRoomId,
                provider,
                providerRoomId,
                MeetingStatus.ENDED,
                startedAt,
                endedAt,
                description);
    }

    /**
     * 외부 실시간 세션 종료를 기준으로 회의를 종료 상태로 정리한다.
     *
     * <p>현재 Meetbowl은 STT/LiveKit 세션 종료가 회의 종료의 사실상 기준이 될 수 있다. 아직 `IN_PROGRESS`로 명시 전환되지 않은 회의라도
     * 실제 미디어 세션이 종료되면 `ENDED`로 정리할 수 있어야 하므로, `SCHEDULED`와 `IN_PROGRESS` 모두 이 경로를 허용한다.
     */
    public Meeting completeFromExternalSession(Instant endedAt) {
        if (status == MeetingStatus.ENDED) {
            return this;
        }
        if (status == MeetingStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "취소된 회의는 종료 처리할 수 없습니다.");
        }
        Instant resolvedStartedAt =
                startedAt != null
                        ? startedAt
                        // 아직 startedAt이 없으면 종료 직전 시점을 시작값으로 보정해 시간 역전을 막는다.
                        : endedAt.minusMillis(1);
        return of(
                id,
                title,
                scheduledAt,
                scheduledEndAt,
                hostUserId,
                meetingRoomId,
                provider,
                providerRoomId,
                MeetingStatus.ENDED,
                resolvedStartedAt,
                endedAt,
                description);
    }

    /** 회의 취소. 이미 종료/취소된 회의는 다시 취소할 수 없다. */
    public Meeting cancel() {
        if (status == MeetingStatus.ENDED || status == MeetingStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "종료되었거나 이미 취소된 회의입니다.");
        }
        return of(
                id,
                title,
                scheduledAt,
                scheduledEndAt,
                hostUserId,
                meetingRoomId,
                provider,
                providerRoomId,
                MeetingStatus.CANCELLED,
                startedAt,
                endedAt,
                description);
    }

    /** 회의 내용 수정(제목·내용·예정 시작/종료시각·회의실). 종료/취소된 회의는 수정할 수 없다. 주최자·상태·진행시각은 보존한다. */
    public Meeting change(
            String newTitle,
            Instant newScheduledAt,
            Instant newScheduledEndAt,
            UUID newMeetingRoomId,
            String newDescription) {
        if (status == MeetingStatus.ENDED || status == MeetingStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "종료되었거나 취소된 회의는 수정할 수 없습니다.");
        }
        return of(
                id,
                newTitle,
                newScheduledAt,
                newScheduledEndAt,
                hostUserId,
                newMeetingRoomId,
                provider,
                providerRoomId,
                status,
                startedAt,
                endedAt,
                newDescription);
    }

    /**
     * 회의 관리자를 다른 참석자로 넘긴다.
     *
     * <p>회의 중 호스트가 자리를 비우더라도 회의가 계속될 수 있도록, 호스트 권한만 갈아끼우고 나머지 진행 상태는 유지한다.
     */
    public Meeting transferHost(UUID newHostUserId) {
        if (status == MeetingStatus.ENDED || status == MeetingStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "종료되었거나 취소된 회의는 관리자를 변경할 수 없습니다.");
        }
        if (newHostUserId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "새 회의 관리자는 필수입니다.");
        }
        if (hostUserId.equals(newHostUserId)) {
            return this;
        }
        return of(
                id,
                title,
                scheduledAt,
                scheduledEndAt,
                newHostUserId,
                meetingRoomId,
                provider,
                providerRoomId,
                status,
                startedAt,
                endedAt,
                description);
    }

    public boolean isHostedBy(UUID userId) {
        return hostUserId.equals(userId);
    }

    public UUID id() {
        return id;
    }

    public String title() {
        return title;
    }

    public Instant scheduledAt() {
        return scheduledAt;
    }

    public Instant scheduledEndAt() {
        return scheduledEndAt;
    }

    public UUID hostUserId() {
        return hostUserId;
    }

    public UUID meetingRoomId() {
        return meetingRoomId;
    }

    public String provider() {
        return provider;
    }

    public String providerRoomId() {
        return providerRoomId;
    }

    public MeetingStatus status() {
        return status;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant endedAt() {
        return endedAt;
    }

    public String description() {
        return description;
    }
}
