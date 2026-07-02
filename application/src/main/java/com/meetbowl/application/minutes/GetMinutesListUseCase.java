package com.meetbowl.application.minutes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesFavoriteRepositoryPort;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;

/** 사용자가 접근할 수 있는 회의록 목록을 조회하고, 사용자별 즐겨찾기 여부를 함께 계산한다. */
@Service
public class GetMinutesListUseCase {

    private final MinutesRepositoryPort minutesRepositoryPort;
    private final MinutesFavoriteRepositoryPort favoriteRepositoryPort;
    private final MeetingAttendeeRepositoryPort attendeeRepositoryPort;
    private final MinutesMeetingMetadataAssembler metadataAssembler;

    public GetMinutesListUseCase(
            MinutesRepositoryPort minutesRepositoryPort,
            MinutesFavoriteRepositoryPort favoriteRepositoryPort,
            MeetingAttendeeRepositoryPort attendeeRepositoryPort,
            MinutesMeetingMetadataAssembler metadataAssembler) {
        this.minutesRepositoryPort = minutesRepositoryPort;
        this.favoriteRepositoryPort = favoriteRepositoryPort;
        this.attendeeRepositoryPort = attendeeRepositoryPort;
        this.metadataAssembler = metadataAssembler;
    }

    @Transactional(readOnly = true)
    public List<MinutesListItemResult> execute(
            UUID actorUserId, UUID actorOrganizationId, String keyword) {
        Set<UUID> accessibleMeetingIds =
                attendeeRepositoryPort.findByUserId(actorUserId).stream()
                        .map(MeetingAttendee::meetingId)
                        .collect(Collectors.toSet());
        List<Minutes> minutes =
                hasKeyword(keyword)
                        ? minutesRepositoryPort.searchByOrganizationIdAndMeetingIds(
                                actorOrganizationId, accessibleMeetingIds, keyword.trim())
                        : minutesRepositoryPort.findByOrganizationIdAndMeetingIds(
                                actorOrganizationId, accessibleMeetingIds);
        Set<UUID> favoriteMinutesIds =
                favoriteRepositoryPort.findByUserId(actorUserId).stream()
                        .map(favorite -> favorite.minutesId())
                        .collect(Collectors.toSet());
        Map<UUID, MinutesMeetingMetadata> metadataByMeetingId =
                metadataAssembler.assemble(
                        minutes.stream().map(Minutes::meetingId).toList(), actorOrganizationId);

        return minutes.stream()
                .filter(item -> MinutesAccessValidator.canRead(item, actorUserId))
                .map(
                        item ->
                                MinutesListItemResult.from(
                                        item,
                                        favoriteMinutesIds.contains(item.id()),
                                        metadataByMeetingId.getOrDefault(
                                                item.meetingId(),
                                                MinutesMeetingMetadata.empty(
                                                        item.reviewerUserId()))))
                .toList();
    }

    private static boolean hasKeyword(String keyword) {
        return keyword != null && !keyword.isBlank();
    }
}
