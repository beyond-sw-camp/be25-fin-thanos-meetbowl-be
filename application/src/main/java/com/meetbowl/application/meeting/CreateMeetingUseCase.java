package com.meetbowl.application.meeting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.AttendeeRole;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;

/**
 * 회의 생성(예약) UseCase다. 회의실을 사용하는 회의는 같은 회의실·겹치는 시간대의 중복 예약을 막고, 주최자와 초대 참석자를 함께 저장한다.
 *
 * <p>동시성 처리: 하나의 트랜잭션 안에서 (1) 회의실 행에 비관적 쓰기 잠금을 걸어 같은 회의실에 대한 동시 요청을 직렬화하고, (2) 잠금이 걸린 상태에서 시간대 겹침을
 * 검사한 뒤, (3) 겹치는 활성 회의가 없을 때만 저장한다. 잠금이 검사~저장 구간을 보호하므로, 같은 회의실·시간을 동시에 요청해도 하나만 성공하고 나머지는
 * {@link ErrorCode#MEETING_ROOM_ALREADY_RESERVED}로 실패한다. 화상회의만 진행하는 회의(회의실 없음)는 잠금/검사 없이 저장한다.
 *
 * <p>참석자: 주최자는 HOST로 자동 포함하고, {@code attendeeUserIds}는 PARTICIPANT로 저장하며, 그중 검토자로 지정된 사용자는
 * REVIEWER로 저장한다. 회의와 참석자는 동일 트랜잭션에서 저장한다. 참석자/검토자 사용자 정보(계열사·부서·팀)는 유저(조직) 도메인 소유이며,
 * 참석자 검색은 ElasticSearch 기반 조직 도메인(F5)에서 처리한다 — 이 UseCase는 확정된 userId만 받고, 사용자 존재/유효성 검증은 조직 도메인 책임이다.
 */
@Service
public class CreateMeetingUseCase {

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort;
    private final MeetingRoomReservationGuard reservationGuard;
    private final ObjectProvider<MeetingCalendarSyncPort> meetingCalendarSyncPortProvider;

    public CreateMeetingUseCase(
            MeetingRepositoryPort meetingRepositoryPort,
            MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort,
            MeetingRoomReservationGuard reservationGuard,
            ObjectProvider<MeetingCalendarSyncPort> meetingCalendarSyncPortProvider) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.meetingAttendeeRepositoryPort = meetingAttendeeRepositoryPort;
        this.reservationGuard = reservationGuard;
        this.meetingCalendarSyncPortProvider = meetingCalendarSyncPortProvider;
    }

    @Transactional
    public MeetingResult execute(CreateMeetingCommand command) {
        // 입력 검증(제목 필수, 예정 시작 < 예정 종료 등)은 도메인 생성 시 수행된다.
        Meeting meeting =
                Meeting.create(
                        command.title(),
                        command.scheduledAt(),
                        command.scheduledEndAt(),
                        command.hostUserId(),
                        command.meetingRoomId(),
                        command.provider(),
                        command.providerRoomId(),
                        command.description());

        if (command.meetingRoomId() != null) {
            // 회의실 예약 가능 검증(락 + 시간 겹침). 생성이므로 제외할 회의는 없다(null).
            reservationGuard.verifyAvailable(
                    meeting.meetingRoomId(),
                    meeting.scheduledAt(),
                    meeting.scheduledEndAt(),
                    null);
        }

        Meeting saved = meetingRepositoryPort.save(meeting);
        List<MeetingAttendee> attendees =
                saveAttendees(
                        saved.id(),
                        command.hostUserId(),
                        command.attendeeUserIds(),
                        command.reviewerUserId());
        syncCalendar(saved, attendees);
        return MeetingResult.of(saved, attendees);
    }

    private void syncCalendar(Meeting meeting, List<MeetingAttendee> attendees) {
        // 회의 저장과 참석자 저장이 모두 성공한 뒤 같은 트랜잭션에서 개인 캘린더로 투영한다.
        // 실제 생성/갱신 멱등성은 개인 워크스페이스 쪽 구현체가 meetingId 기준으로 보장한다(있으면 갱신, 없으면 생성).
        // ifAvailable: MeetingCalendarSyncPort 구현체(개인 워크스페이스 모듈)가 아직 없으면 조용히 건너뛴다 → 회의 기능엔 영향 없음.
        meetingCalendarSyncPortProvider.ifAvailable(
                calendarSync ->
                        calendarSync.syncFromMeeting(
                                new MeetingCalendarSyncCommand(
                                        meeting.id(),
                                        attendees.stream().map(MeetingAttendee::userId).toList(),
                                        meeting.title(),
                                        null,
                                        meeting.scheduledAt(),
                                        meeting.scheduledEndAt())));
    }

    /**
     * 주최자(HOST), 초대 참석자(PARTICIPANT), 회의록 검토자(REVIEWER)를 저장한다.
     *
     * <p>참석자 출처(경계): {@code attendeeUserIds}는 유저(조직) 도메인 사용자 테이블에서 선택된 사용자다. 계열사/부서/팀 기준 참석자
     * "검색"은 ElasticSearch 기반 조직 도메인 검색(F5)에서 처리하며, 이 UseCase는 이미 확정된 userId만 받는다. 따라서 사용자의 존재/유효성
     * (탈퇴·비활성 등) 검증은 조직 도메인 책임이고, 여기서는 회의 도메인 규칙(역할 배정·중복 제거·검토자 제약)만 책임진다.
     *
     * <p>검토자 규칙: 회의록 검토자는 반드시 "참석자 중에서" 지정한다. 검토자로 지정된 참석자만 REVIEWER 역할을 갖고 나머지는 PARTICIPANT다.
     * 참석자 1명당 역할은 하나이므로, 주최자(HOST)는 검토자로 지정할 수 없다(participants에서 제외되어 검증에 걸린다).
     */
    private List<MeetingAttendee> saveAttendees(
            UUID meetingId, UUID hostUserId, List<UUID> attendeeUserIds, UUID reviewerUserId) {
        List<MeetingAttendee> attendees = new ArrayList<>();
        // 주최자 = 생성 요청자. 회의당 1명, 항상 HOST로 포함한다.
        attendees.add(MeetingAttendee.create(meetingId, hostUserId, AttendeeRole.HOST));

        // 초대 참석자: 중복 제거 + 주최자 제외(이미 HOST로 포함됨).
        Set<UUID> participants = new LinkedHashSet<>();
        if (attendeeUserIds != null) {
            participants.addAll(attendeeUserIds);
        }
        participants.remove(hostUserId);

        // 검토자 제약: 검토자는 참석자(주최자 제외) 중에서만 지정할 수 있다(회의 도메인 규칙).
        if (reviewerUserId != null && !participants.contains(reviewerUserId)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "회의록 검토자는 참석자 중에서 지정해야 합니다.");
        }

        for (UUID userId : participants) {
            // 검토자로 지정된 참석자만 REVIEWER, 나머지는 PARTICIPANT.
            AttendeeRole role =
                    userId.equals(reviewerUserId)
                            ? AttendeeRole.REVIEWER
                            : AttendeeRole.PARTICIPANT;
            attendees.add(MeetingAttendee.create(meetingId, userId, role));
        }
        return meetingAttendeeRepositoryPort.saveAll(attendees);
    }
}
