package com.meetbowl.infrastructure.persistence.meeting;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.meeting.AttendanceStatus;
import com.meetbowl.domain.meeting.AttendeeRole;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 회의 참석자 JPA Entity다. {@code meeting_attendee} 테이블과 1:1로 매핑된다. (meeting_id, user_id) 유니크로 동일 회의 중복
 * 참석자 등록을 차단하고, (user_id, meeting_id) 인덱스로 "내 회의" 조회를 지원한다.
 */
@Entity
@Table(
        name = "meeting_attendees",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_meeting_attendee_meeting_user",
                        columnNames = {"meeting_id", "user_id"}),
        indexes = {@Index(name = "idx_meeting_attendee_user", columnList = "user_id, meeting_id")})
public class MeetingAttendeeEntity extends BaseEntity {

    /** 소속 회의(FK). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID meetingId;

    /** 참석 사용자(FK). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    /** 회의 내 역할(HOST/PARTICIPANT/REVIEWER). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendeeRole role;

    /** 참석 응답 상태(INVITED/ACCEPTED/DECLINED). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus attendanceStatus;

    protected MeetingAttendeeEntity() {}

    private MeetingAttendeeEntity(
            UUID meetingId, UUID userId, AttendeeRole role, AttendanceStatus attendanceStatus) {
        this.meetingId = meetingId;
        this.userId = userId;
        this.role = role;
        this.attendanceStatus = attendanceStatus;
    }

    static MeetingAttendeeEntity from(MeetingAttendee attendee) {
        MeetingAttendeeEntity entity =
                new MeetingAttendeeEntity(
                        attendee.meetingId(),
                        attendee.userId(),
                        attendee.role(),
                        attendee.attendanceStatus());
        entity.setId(attendee.id());
        return entity;
    }

    MeetingAttendee toDomain() {
        return MeetingAttendee.of(getId(), meetingId, userId, role, attendanceStatus);
    }
}
