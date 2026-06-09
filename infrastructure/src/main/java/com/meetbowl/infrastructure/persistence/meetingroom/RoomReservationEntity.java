package com.meetbowl.infrastructure.persistence.meetingroom;

import com.meetbowl.domain.meetingroom.ReservationStatus;
import com.meetbowl.domain.meetingroom.RoomReservation;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

// 회의실 예약. db table과 1대1 매칭
/**
 * 회의실 예약 JPA Entity다. 명세(meeting-reservation.md) §4.1 {@code room_reservation} 테이블과 1:1로 매핑된다.
 * Entity는 infrastructure 내부 모델이며 API/Application/Domain 계층으로 노출하지 않는다.
 *
 * <p>"회의실 예약 현황" 화면(시간축×회의실 격자)과 "새 회의 예약" 모달의 데이터 소스다. 모달 입력 ↔ 컬럼 매핑은 다음과 같다.
 *
 * <ul>
 *   <li>회의 제목 → {@code title}
 *   <li>회의실 → {@code meetingRoomId}
 *   <li>날짜 + 시작/종료 시간(ISO-8601 UTC) → {@code startedAt} / {@code endedAt}
 *   <li>회의 내용 → {@code description}
 * </ul>
 *
 * <p>모달의 <b>참석자(attendeeUserIds)와 첨부파일은 이 테이블이 소유하지 않는다.</b> 명세상 각각 회의(Meeting) 도메인의 {@code
 * meeting_attendee} / {@code meeting_attachment} 테이블 소관이므로 여기 두지 않는다(이번 범위 밖). 회의와 함께 예약된 경우에만
 * {@code meetingId}로 0..1 연결하고, 회의 없는 단순 회의실 점유는 {@code meetingId=null}이다.
 *
 * <p>회의실/예약자/회의 참조는 모듈 간 직접 연관 대신 raw UUID 컬럼으로만 둔다. (meeting_room_id, started_at, ended_at) 인덱스는
 * 중복 예약 겹침 조회를 위한 것이며, 최종 중복 방지 보장 메커니즘은 명세 F4(서비스 트랜잭션 + 잠금/제약)를 따른다.
 *
 * <p>조회 화면의 4종 상태 색상은 이 테이블만으로는 완성되지 않는다. "예약됨 / 내 예약"은 {@code status=RESERVED}와 {@code
 * reservedByUserId}(=현재 사용자) 비교로 도출하지만, "사용 제한"은 {@code meeting_room.is_available}, 그리고 사이트/건물
 * 그룹핑·회의실명·정원은 기준정보(MeetingRoom/Building/Site) 소관이다(조회 시 조인, 이번 범위 밖).
 */
@Entity
@Table(
        name = "room_reservation",
        indexes = {
            @Index(
                    name = "idx_room_reservation_room_period",
                    columnList = "meeting_room_id, started_at, ended_at")
        })
public class RoomReservationEntity extends BaseEntity {

    /** 예약 대상 회의실(FK). 회의실 아이디 (어느 회의실 예약했는지) */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID meetingRoomId;

    /** 예약자(FK) = 예약을 생성한 로그인 사용자. 격자 블록의 "예약자명"과 조회 화면 "내 예약" 판별 기준. */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID reservedByUserId;

    /**
     * 연결된 회의id(FK, nullable. 어느 회의와 연결되는가). 회의와 함께 예약된 경우에만 채워지고, 단순 회의실 점유는 null. 참석자/첨부는 이 회의 쪽이
     * 소유한다.
     */
    @Column(columnDefinition = "BINARY(16)")
    private UUID meetingId;

    /** 회의 제목 */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 회의 내용(모달 "회의 내용"). md §4.1 테이블 표에는 없으나 F4 예약 생성 요청 DTO에는 존재하여, 단순 예약에서도 입력값을 보존하기 위해 유지한다(회의
     * 연결 전까지의 메모). > TODO: md §4.1 컬럼표와 F4 요청 DTO의 description 불일치 정합 확인.
     */
    @Column(length = 2000)
    private String description;

    /** 예약 시작 시각(UTC 저장, API/이벤트는 ISO-8601 UTC). 가로축 시작점. */
    @Column(nullable = false)
    private Instant startedAt;

    /** 예약 종료 시각(UTC). startedAt < endedAt 불변식은 도메인(RoomReservation)에서 검증한다. */
    @Column(nullable = false)
    private Instant endedAt;

    /** 예약 상태(RESERVED/CANCELLED). 취소는 행 삭제 대신 상태 전환(soft cancel)으로 처리한다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    protected RoomReservationEntity() {}

    private RoomReservationEntity(
            UUID meetingRoomId,
            UUID reservedByUserId,
            UUID meetingId,
            String title,
            String description,
            Instant startedAt,
            Instant endedAt,
            ReservationStatus status) {
        this.meetingRoomId = meetingRoomId;
        this.reservedByUserId = reservedByUserId;
        this.meetingId = meetingId;
        this.title = title;
        this.description = description;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.status = status;
    }

    static RoomReservationEntity from(RoomReservation reservation) {
        RoomReservationEntity entity =
                new RoomReservationEntity(
                        reservation.meetingRoomId(),
                        reservation.reservedByUserId(),
                        reservation.meetingId(),
                        reservation.title(),
                        reservation.description(),
                        reservation.startedAt(),
                        reservation.endedAt(),
                        reservation.status());
        entity.setId(reservation.id());
        return entity;
    }

    RoomReservation toDomain() {
        return RoomReservation.of(
                getId(),
                meetingRoomId,
                reservedByUserId,
                meetingId,
                title,
                description,
                startedAt,
                endedAt,
                status);
    }
}
