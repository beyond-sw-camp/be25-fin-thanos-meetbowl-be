package com.meetbowl.domain.meeting;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 회의 참석자 도메인 모델이다.
 *
 * <p>회의 ↔ 사용자(N:M) 연결 한 건을 표현한다. "참석자가 여러 명"이라 회의 컬럼이 아닌 별도 행으로 관리하며, 연결마다 역할({@link
 * AttendeeRole})과 참석 응답 상태({@link AttendanceStatus})를 가진다. 회의는 {@code meetingId}, 사용자는 {@code
 * userId} raw UUID로 참조한다.
 */
public class MeetingAttendee {

    private final UUID id;

    /** 소속 회의(FK). */
    private final UUID meetingId;

    /** 참석 사용자(FK). */
    private final UUID userId;

    /** 회의 내 역할(HOST/PARTICIPANT/REVIEWER). */
    private final AttendeeRole role;

    /** 참석 응답 상태(INVITED/ACCEPTED/DECLINED). */
    private final AttendanceStatus attendanceStatus;

    private MeetingAttendee(
            UUID id,
            UUID meetingId,
            UUID userId,
            AttendeeRole role,
            AttendanceStatus attendanceStatus) {
        this.id = id;
        this.meetingId = meetingId;
        this.userId = userId;
        this.role = role;
        this.attendanceStatus = attendanceStatus;
    }

    public static MeetingAttendee create(UUID meetingId, UUID userId, AttendeeRole role) {
        return of(null, meetingId, userId, role, AttendanceStatus.INVITED);
    }

    public static MeetingAttendee of(
            UUID id,
            UUID meetingId,
            UUID userId,
            AttendeeRole role,
            AttendanceStatus attendanceStatus) {
        if (meetingId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의는 필수입니다.");
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "참석자 사용자는 필수입니다.");
        }
        if (role == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "참석자 역할은 필수입니다.");
        }
        if (attendanceStatus == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "참석 상태는 필수입니다.");
        }
        return new MeetingAttendee(id, meetingId, userId, role, attendanceStatus);
    }

    public MeetingAttendee respond(AttendanceStatus newStatus) {
        return of(id, meetingId, userId, role, newStatus);
    }

    public UUID id() {
        return id;
    }

    public UUID meetingId() {
        return meetingId;
    }

    public UUID userId() {
        return userId;
    }

    public AttendeeRole role() {
        return role;
    }

    public AttendanceStatus attendanceStatus() {
        return attendanceStatus;
    }
}
