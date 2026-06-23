package com.meetbowl.application.minutes;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.organization.Department;
import com.meetbowl.domain.organization.DepartmentRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

/** 회의록 목록/상세 응답에 필요한 회의 표시용 메타데이터를 조립한다. */
@Component
public class MinutesMeetingMetadataAssembler {

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final MeetingAttendeeRepositoryPort attendeeRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final DepartmentRepositoryPort departmentRepositoryPort;

    public MinutesMeetingMetadataAssembler(
            MeetingRepositoryPort meetingRepositoryPort,
            MeetingAttendeeRepositoryPort attendeeRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            DepartmentRepositoryPort departmentRepositoryPort) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.attendeeRepositoryPort = attendeeRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.departmentRepositoryPort = departmentRepositoryPort;
    }

    public Map<UUID, MinutesMeetingMetadata> assemble(
            Collection<UUID> meetingIds, UUID organizationId) {
        if (meetingIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Meeting> meetings =
                meetingRepositoryPort.findByIds(meetingIds).stream()
                        .collect(Collectors.toMap(Meeting::id, Function.identity()));
        Map<UUID, List<MeetingAttendee>> attendeesByMeetingId =
                attendeeRepositoryPort.findByMeetingIds(meetingIds).stream()
                        .collect(Collectors.groupingBy(MeetingAttendee::meetingId));

        Set<UUID> userIds =
                attendeesByMeetingId.values().stream()
                        .flatMap(List::stream)
                        .map(MeetingAttendee::userId)
                        .collect(Collectors.toSet());
        meetings.values().stream().map(Meeting::hostUserId).forEach(userIds::add);

        Map<UUID, User> usersById =
                userRepositoryPort.findAllByAffiliateId(organizationId).stream()
                        .filter(user -> userIds.contains(user.id()))
                        .collect(Collectors.toMap(User::id, Function.identity(), (left, right) -> left));
        Map<UUID, String> departmentNamesById = departmentNames(usersById.values());

        return meetingIds.stream()
                .collect(
                        Collectors.toMap(
                                Function.identity(),
                                meetingId ->
                                        assembleOne(
                                                meetings.get(meetingId),
                                                attendeesByMeetingId.getOrDefault(
                                                        meetingId, List.of()),
                                                usersById,
                                                departmentNamesById)));
    }

    public MinutesMeetingMetadata assemble(
            UUID meetingId, UUID organizationId, UUID fallbackReviewerUserId) {
        return assemble(List.of(meetingId), organizationId)
                .getOrDefault(meetingId, MinutesMeetingMetadata.empty(fallbackReviewerUserId));
    }

    private MinutesMeetingMetadata assembleOne(
            Meeting meeting,
            List<MeetingAttendee> attendees,
            Map<UUID, User> usersById,
            Map<UUID, String> departmentNamesById) {
        UUID reviewerUserId =
                attendees.stream()
                        .filter(MeetingAttendee::reviewer)
                        .map(MeetingAttendee::userId)
                        .findFirst()
                        .orElse(null);
        User reviewer = reviewerUserId == null ? null : usersById.get(reviewerUserId);
        String reviewerDepartment =
                reviewer == null || reviewer.departmentId() == null
                        ? null
                        : departmentNamesById.get(reviewer.departmentId());
        return new MinutesMeetingMetadata(
                meeting == null ? null : meeting.title(),
                meeting == null ? null : meeting.startedAt(),
                meeting == null ? null : meeting.endedAt(),
                attendees.size(),
                reviewerUserId,
                reviewer == null ? null : reviewer.name(),
                reviewerDepartment);
    }

    private Map<UUID, String> departmentNames(Collection<User> users) {
        Set<UUID> departmentIds =
                users.stream()
                        .map(User::departmentId)
                        .filter(id -> id != null)
                        .collect(Collectors.toSet());
        if (departmentIds.isEmpty()) {
            return Map.of();
        }
        return departmentRepositoryPort.findAllByIds(departmentIds).stream()
                .collect(Collectors.toMap(Department::id, Department::name));
    }
}
