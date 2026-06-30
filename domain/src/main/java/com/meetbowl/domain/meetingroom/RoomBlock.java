package com.meetbowl.domain.meetingroom;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 회의실 시간대 차단 도메인 모델이다.
 *
 * <p>관리자가 회의실의 특정 시간 구간 {@code [startAt, endAt)}을 점검·행사 등의 사유로 예약 불가로 막는다. 사용자가 그 구간과 겹치는 예약을 시도하면 예약 가드가
 * 차단한다. 회의실 전체 비활성화({@link MeetingRoom#isAvailable()})와 달리, 시간대 단위의 부분 차단을 표현한다. 구간은 반개구간으로 다루어 경계
 * 시각(종료 == 다음 시작)이 겹침으로 잡히지 않게 한다.
 */
public class RoomBlock {

    private final UUID id;

    /** 차단 대상 회의실(FK). */
    private final UUID roomId;

    /** 차단 시작 시각(포함, UTC). */
    private final Instant startAt;

    /** 차단 종료 시각(제외, UTC). */
    private final Instant endAt;

    /** 차단 사유(선택, 예: 시설 점검). */
    private final String reason;

    private RoomBlock(UUID id, UUID roomId, Instant startAt, Instant endAt, String reason) {
        this.id = id;
        this.roomId = roomId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.reason = reason;
    }

    /** 신규 차단 생성(id는 저장 시 부여). */
    public static RoomBlock create(UUID roomId, Instant startAt, Instant endAt, String reason) {
        return of(null, roomId, startAt, endAt, reason);
    }

    public static RoomBlock of(UUID id, UUID roomId, Instant startAt, Instant endAt, String reason) {
        if (roomId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "차단 대상 회의실은 필수입니다.");
        }
        if (startAt == null || endAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "차단 시작·종료 시각은 필수입니다.");
        }
        if (!endAt.isAfter(startAt)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "차단 종료 시각은 시작 시각보다 뒤여야 합니다.");
        }
        String normalizedReason = (reason == null || reason.isBlank()) ? null : reason.trim();
        return new RoomBlock(id, roomId, startAt, endAt, normalizedReason);
    }

    /**
     * 주어진 구간 {@code [from, to)}와 이 차단이 겹치는지 판정한다. 반개구간 겹침 규칙({@code startAt < to && from < endAt})을 사용해, 한
     * 구간의 종료 시각과 다른 구간의 시작 시각이 같은 경우(맞닿음)는 겹침으로 보지 않는다.
     */
    public boolean overlaps(Instant from, Instant to) {
        if (from == null || to == null) {
            return false;
        }
        return startAt.isBefore(to) && from.isBefore(endAt);
    }

    public UUID id() {
        return id;
    }

    public UUID roomId() {
        return roomId;
    }

    public Instant startAt() {
        return startAt;
    }

    public Instant endAt() {
        return endAt;
    }

    public String reason() {
        return reason;
    }
}
