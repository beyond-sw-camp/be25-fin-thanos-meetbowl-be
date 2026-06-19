package com.meetbowl.application.meeting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.AttendeeRole;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;

/** 회의 조회 UseCase다. FE의 "전체 / 내가 주최한 회의 / 초대된 회의" 탭에 대응하는 목록 조회와, 권한 검증을 포함한 단건 상세 조회를 제공한다. */
@Service
public class GetMeetingUseCase {

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort;

    public GetMeetingUseCase(
            MeetingRepositoryPort meetingRepositoryPort,
            MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.meetingAttendeeRepositoryPort = meetingAttendeeRepositoryPort;
    }

    /**
     * 내 회의 목록 조회. {@code filter}로 전체/주최/초대를 구분하고, {@code from}/{@code to}(null이면 무시)로 예정 시작 시각 범위를
     * 거른 뒤 최신순(예정 시작 내림차순)으로 반환한다.
     */
    @Transactional(readOnly = true)
    public List<MeetingResult> getMyMeetings(
            UUID userId, MeetingListFilter filter, Instant from, Instant to) {
        List<Meeting> meetings =
                switch (filter) {
                    case HOST -> hostMeetings(userId);
                    case INVITED -> invitedMeetings(userId);
                    case ALL -> union(hostMeetings(userId), invitedMeetings(userId));
                };

        List<Meeting> visible =
                meetings.stream()
                        .filter(meeting -> withinRange(meeting, from, to))
                        .sorted(Comparator.comparing(Meeting::scheduledAt).reversed())
                        .toList();

        // N+1 방지: 회의별로 참석자를 따로 조회하지 않고, 회의 id를 모아 한 번에 배치 조회한 뒤 회의별로 그룹핑한다.
        Map<UUID, List<MeetingAttendee>> attendeesByMeetingId =
                meetingAttendeeRepositoryPort
                        .findByMeetingIds(visible.stream().map(Meeting::id).toList())
                        .stream()
                        .collect(Collectors.groupingBy(MeetingAttendee::meetingId));

        return visible.stream()
                .map(
                        meeting ->
                                MeetingResult.of(
                                        meeting,
                                        attendeesByMeetingId.getOrDefault(meeting.id(), List.of())))
                .toList();
    }

    /** 회의 단건 상세 조회. 주최자/참석자/Admin만 조회할 수 있다. 존재하지 않으면 404. */
    @Transactional(readOnly = true)
    public MeetingResult getById(UUID meetingId, UUID requesterId, boolean isAdmin) {
        Meeting meeting =
                meetingRepositoryPort
                        .findById(meetingId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        List<MeetingAttendee> attendees = meetingAttendeeRepositoryPort.findByMeetingId(meetingId);

        boolean isAttendee = attendees.stream().anyMatch(a -> a.userId().equals(requesterId));
        if (!isAdmin && !meeting.isHostedBy(requesterId) && !isAttendee) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "회의 주최자나 참석자만 조회할 수 있습니다.");
        }
        return MeetingResult.of(meeting, attendees);
    }

    private List<Meeting> hostMeetings(UUID userId) {
        return meetingRepositoryPort.findByHostUserId(userId);
    }

    /** 내가 참석자(주최 제외)로 초대된 회의. 주최자는 HOST 참석자로도 저장되므로 HOST 역할은 제외한다. */
    private List<Meeting> invitedMeetings(UUID userId) {
        List<Meeting> meetings = new ArrayList<>();
        meetingAttendeeRepositoryPort.findByUserId(userId).stream()
                .filter(attendee -> attendee.role() != AttendeeRole.HOST)
                .map(MeetingAttendee::meetingId)
                .distinct()
                .forEach(
                        meetingId ->
                                meetingRepositoryPort.findById(meetingId).ifPresent(meetings::add));
        return meetings;
    }

    /** 주최·초대 회의를 회의 id 기준으로 중복 없이 합친다. */
    private List<Meeting> union(List<Meeting> hosted, List<Meeting> invited) {
        Map<UUID, Meeting> byId = new LinkedHashMap<>();
        hosted.forEach(meeting -> byId.put(meeting.id(), meeting));
        invited.forEach(meeting -> byId.putIfAbsent(meeting.id(), meeting));
        return new ArrayList<>(byId.values());
    }

    private boolean withinRange(Meeting meeting, Instant from, Instant to) {
        Instant scheduledAt = meeting.scheduledAt();
        if (from != null && scheduledAt.isBefore(from)) {
            return false;
        }
        return to == null || !scheduledAt.isAfter(to);
    }
}
