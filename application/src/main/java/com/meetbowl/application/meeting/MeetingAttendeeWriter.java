package com.meetbowl.application.meeting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.AttendeeRole;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;

/**
 * 회의 참석자(주최자/참석자/검토자) 저장을 담당하는 공용 컴포넌트다. 회의 생성·수정이 동일한 역할 배정·검토자 제약 규칙을 공유하도록 한 곳으로 모았다.
 *
 * <p>역할 규칙: 주최자는 HOST 신분으로 자동 포함하고, {@code attendeeUserIds}는 PARTICIPANT 신분으로 저장한다. 검토자는 신분과
 * 독립적인 {@code reviewer} 플래그로 표시하며, 주최자·일반 참석자 누구나 될 수 있다(주최자=검토자 허용). 검토자는 반드시 참석자(주최자 포함) 중에서
 * 지정해야 한다.
 *
 * <p>참석자 출처(경계): {@code attendeeUserIds}는 유저(조직) 도메인에서 이미 확정된 userId만 받는다. 계열사/부서/팀 기준 참석자 "검색"은
 * ElasticSearch 기반 조직 도메인 검색(F5)에서 처리하며, 사용자의 존재/유효성(탈퇴·비활성) 검증은 조직 도메인 책임이다. 여기서는 회의 도메인 규칙(역할
 * 배정·중복 제거·검토자 제약)만 책임진다.
 */
@Component
public class MeetingAttendeeWriter {

    private final MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort;

    public MeetingAttendeeWriter(MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort) {
        this.meetingAttendeeRepositoryPort = meetingAttendeeRepositoryPort;
    }

    /** 신규 저장(회의 생성). 주최자(HOST) + 초대 참석자(PARTICIPANT)를 저장하고, 검토자에게 reviewer 플래그를 단다. */
    public List<MeetingAttendee> save(
            UUID meetingId, UUID hostUserId, List<UUID> attendeeUserIds, UUID reviewerUserId) {
        return meetingAttendeeRepositoryPort.saveAll(
                build(meetingId, hostUserId, attendeeUserIds, reviewerUserId));
    }

    /**
     * 전체 교체(회의 수정). 기존 참석자를 모두 삭제한 뒤 새 목록으로 재저장한다. 검토자 제약은 새 참석자 목록 기준으로 재검증되므로, 참석자를 바꿔 기존 검토자가
     * 빠지면 여기서 걸린다.
     *
     * <p>삭제는 즉시 flush되는 bulk delete({@code deleteByMeetingId})로 실행되어, (meeting_id, user_id) 유니크 제약과
     * 충돌 없이 같은 트랜잭션에서 재삽입된다.
     */
    public List<MeetingAttendee> replace(
            UUID meetingId, UUID hostUserId, List<UUID> attendeeUserIds, UUID reviewerUserId) {
        meetingAttendeeRepositoryPort.deleteByMeetingId(meetingId);
        return save(meetingId, hostUserId, attendeeUserIds, reviewerUserId);
    }

    private List<MeetingAttendee> build(
            UUID meetingId, UUID hostUserId, List<UUID> attendeeUserIds, UUID reviewerUserId) {
        // 초대 참석자: 중복 제거 + 주최자 제외(주최자는 HOST 신분으로 따로 포함한다).
        Set<UUID> participants = new LinkedHashSet<>();
        if (attendeeUserIds != null) {
            participants.addAll(attendeeUserIds);
        }
        participants.remove(hostUserId);

        // 검토자 제약: 검토자는 참석자 중에서 지정해야 한다. 신분과 무관하므로 주최자도 검토자가 될 수 있다.
        boolean reviewerIsHost = reviewerUserId != null && reviewerUserId.equals(hostUserId);
        if (reviewerUserId != null && !reviewerIsHost && !participants.contains(reviewerUserId)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "회의록 검토자는 참석자 중에서 지정해야 합니다.");
        }

        List<MeetingAttendee> attendees = new ArrayList<>();
        // 주최자 = 회의 호스트. 회의당 1명, 항상 HOST 신분으로 포함하며 검토자로 지정됐으면 reviewer 플래그를 단다.
        attendees.add(MeetingAttendee.create(meetingId, hostUserId, AttendeeRole.HOST, reviewerIsHost));

        for (UUID userId : participants) {
            // 신분은 모두 PARTICIPANT, 검토자로 지정된 1명만 reviewer 플래그가 true.
            boolean reviewer = userId.equals(reviewerUserId);
            attendees.add(
                    MeetingAttendee.create(meetingId, userId, AttendeeRole.PARTICIPANT, reviewer));
        }
        return attendees;
    }
}
