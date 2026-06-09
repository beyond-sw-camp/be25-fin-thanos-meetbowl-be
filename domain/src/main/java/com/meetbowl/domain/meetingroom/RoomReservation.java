package com.meetbowl.domain.meetingroom;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 회의실 예약 도메인 모델이다.
 *
 * <p>"회의실 예약 현황" 화면(시간축×회의실 격자)과 "새 회의 예약" 모달이 다루는 예약 한 건을 표현한다. 모달 입력 ↔ 필드 매핑: 회의 제목→title,
 * 회의실→meetingRoomId, 날짜/시작·종료(ISO-8601 UTC)→startedAt/endedAt, 회의 내용→description. 모달의
 * 참석자(attendeeUserIds)와 첨부파일은 이 모델이 아니라 회의(Meeting) 도메인이 소유하므로 여기 두지 않는다. 회의와 함께 예약된 경우에만
 * meetingId로 0..1 연결한다.
 *
 * <p>같은 회의실의 겹치는 시간대 중복 예약 방지가 핵심 불변식이다. 예약 겹침 판정은 {@link #overlaps(Instant, Instant)}로 도메인에서 표현하고,
 * 실제 정합성 보장은 서비스 트랜잭션 + DB 제약과 함께 수행한다(명세 F4 참고). 불변 객체로 다루며 상태 변경은 새 인스턴스를 반환한다.
 */
public class RoomReservation {

    private final UUID id;

    /** 예약 대상 회의실(FK). */
    private final UUID meetingRoomId;

    /** 예약자 = 예약 생성 사용자. 격자 블록의 "예약자명"과 "내 예약" 판별 기준. */
    private final UUID reservedByUserId;

    /** 연결된 회의(nullable). 단순 회의실 점유는 null. 참석자/첨부는 이 회의가 소유한다. */
    private final UUID meetingId;

    /** 회의 제목(필수). */
    private final String title;

    /** 회의 내용(선택). */
    private final String description;

    /** 예약 시작 시각(UTC). */
    private final Instant startedAt;

    /** 예약 종료 시각(UTC). startedAt < endedAt 불변식을 생성 시 검증한다. */
    private final Instant endedAt;

    /** 예약 상태(RESERVED/CANCELLED). 취소는 soft cancel. */
    private final ReservationStatus status;

    private RoomReservation(
            UUID id,
            UUID meetingRoomId,
            UUID reservedByUserId,
            UUID meetingId,
            String title,
            String description,
            Instant startedAt,
            Instant endedAt,
            ReservationStatus status) {
        this.id = id;
        this.meetingRoomId = meetingRoomId;
        this.reservedByUserId = reservedByUserId;
        this.meetingId = meetingId;
        this.title = title;
        this.description = description;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.status = status;
    }

    public static RoomReservation create(
            UUID meetingRoomId,
            UUID reservedByUserId,
            UUID meetingId,
            String title,
            String description,
            Instant startedAt,
            Instant endedAt) {
        return of(
                null,
                meetingRoomId,
                reservedByUserId,
                meetingId,
                title,
                description,
                startedAt,
                endedAt,
                ReservationStatus.RESERVED);
    }

    public static RoomReservation of(
            UUID id,
            UUID meetingRoomId,
            UUID reservedByUserId,
            UUID meetingId,
            String title,
            String description,
            Instant startedAt,
            Instant endedAt,
            ReservationStatus status) {
        if (meetingRoomId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의실은 필수입니다.");
        }
        if (reservedByUserId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "예약자는 필수입니다.");
        }
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의 제목은 필수입니다.");
        }
        if (startedAt == null || endedAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "예약 시작/종료 시간은 필수입니다.");
        }
        if (!startedAt.isBefore(endedAt)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "시작 시간은 종료 시간보다 이전이어야 합니다.");
        }
        if (status == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "예약 상태는 필수입니다.");
        }
        return new RoomReservation(
                id,
                meetingRoomId,
                reservedByUserId,
                meetingId,
                title,
                description,
                startedAt,
                endedAt,
                status);
    }

    /** 이 예약이 주어진 시간대와 겹치는지 판정한다. 취소된 예약은 점유로 보지 않으며, 경계가 맞닿는 경우(이전 종료 == 다음 시작)는 겹침으로 보지 않는다. */
    public boolean overlaps(Instant otherStartedAt, Instant otherEndedAt) {
        return isActive() && startedAt.isBefore(otherEndedAt) && endedAt.isAfter(otherStartedAt);
    }

    public boolean isActive() {
        return status == ReservationStatus.RESERVED;
    }

    public boolean isOwnedBy(UUID userId) {
        return reservedByUserId.equals(userId);
    }

    /** 예약 취소(soft cancel). 이미 취소된 예약을 다시 취소하면 상태 충돌로 처리한다. (FR-022) */
    public RoomReservation cancel() {
        if (!isActive()) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "이미 취소된 예약입니다.");
        }
        return new RoomReservation(
                id,
                meetingRoomId,
                reservedByUserId,
                meetingId,
                title,
                description,
                startedAt,
                endedAt,
                ReservationStatus.CANCELLED);
    }

    /** 예약 정보 수정. 회의실/시간 변경 시 호출 측에서 중복 예약을 재검증해야 한다. */
    public RoomReservation change(
            UUID newMeetingRoomId,
            String newTitle,
            String newDescription,
            Instant newStartedAt,
            Instant newEndedAt) {
        if (!isActive()) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "취소된 예약은 수정할 수 없습니다.");
        }
        return of(
                id,
                newMeetingRoomId,
                reservedByUserId,
                meetingId,
                newTitle,
                newDescription,
                newStartedAt,
                newEndedAt,
                status);
    }

    public UUID id() {
        return id;
    }

    public UUID meetingRoomId() {
        return meetingRoomId;
    }

    public UUID reservedByUserId() {
        return reservedByUserId;
    }

    public UUID meetingId() {
        return meetingId;
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

    public ReservationStatus status() {
        return status;
    }
}
