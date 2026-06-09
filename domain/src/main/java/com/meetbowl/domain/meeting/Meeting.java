package com.meetbowl.domain.meeting;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 회의 본체 도메인 모델이다.
 *
 * <p>"새 회의 예약" 폼이 만드는 회의 한 건을 표현한다. 회의실 점유(시간/회의실/중복방지)는 {@code RoomReservation}이, 회의 내용/날짜/시간은 예약이
 * 함께 보관하고, 이 모델은 회의의 주최자·화상 provider 연결·진행 상태를 소유한다. 참석자와 첨부파일은 별도 도메인 모델({@link MeetingAttendee},
 * {@link MeetingAttachment})이 회의를 {@code meetingId}로 참조해 소유한다.
 *
 * <p>불변 객체로 다루며 상태 전환은 새 인스턴스를 반환한다. 다른 도메인(사용자/회의실)은 raw UUID 참조로만 연결한다.
 */
public class Meeting {

    private final UUID id;

    /** 회의 제목(필수). */
    private final String title;

    /** 예정 시각(필수). 실제 시작 전까지 기준이 되는 일정 시각. */
    private final Instant scheduledAt;

    /** 주최자 사용자(FK). 회의 생성 요청자이며 참석자 중 HOST 역할. */
    private final UUID hostUserId;

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

    private Meeting(
            UUID id,
            String title,
            Instant scheduledAt,
            UUID hostUserId,
            String provider,
            String providerRoomId,
            MeetingStatus status,
            Instant startedAt,
            Instant endedAt) {
        this.id = id;
        this.title = title;
        this.scheduledAt = scheduledAt;
        this.hostUserId = hostUserId;
        this.provider = provider;
        this.providerRoomId = providerRoomId;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public static Meeting create(
            String title,
            Instant scheduledAt,
            UUID hostUserId,
            String provider,
            String providerRoomId) {
        return of(
                null,
                title,
                scheduledAt,
                hostUserId,
                provider,
                providerRoomId,
                MeetingStatus.SCHEDULED,
                null,
                null);
    }

    public static Meeting of(
            UUID id,
            String title,
            Instant scheduledAt,
            UUID hostUserId,
            String provider,
            String providerRoomId,
            MeetingStatus status,
            Instant startedAt,
            Instant endedAt) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의 제목은 필수입니다.");
        }
        if (scheduledAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의 예정 시각은 필수입니다.");
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
                hostUserId,
                provider,
                providerRoomId,
                status,
                startedAt,
                endedAt);
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
                hostUserId,
                provider,
                providerRoomId,
                MeetingStatus.IN_PROGRESS,
                startedAt,
                endedAt);
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
                hostUserId,
                provider,
                providerRoomId,
                MeetingStatus.ENDED,
                startedAt,
                endedAt);
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
                hostUserId,
                provider,
                providerRoomId,
                MeetingStatus.CANCELLED,
                startedAt,
                endedAt);
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

    public UUID hostUserId() {
        return hostUserId;
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
}
