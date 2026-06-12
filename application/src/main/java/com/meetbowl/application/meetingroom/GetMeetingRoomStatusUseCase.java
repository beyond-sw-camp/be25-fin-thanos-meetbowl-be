package com.meetbowl.application.meetingroom;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.meeting.MeetingStatus;
import com.meetbowl.domain.meetingroom.Building;
import com.meetbowl.domain.meetingroom.BuildingRepositoryPort;
import com.meetbowl.domain.meetingroom.MeetingRoom;
import com.meetbowl.domain.meetingroom.MeetingRoomRepositoryPort;
import com.meetbowl.domain.meetingroom.Site;
import com.meetbowl.domain.meetingroom.SiteRepositoryPort;

/**
 * 회의실 현황 조회 UseCase다(F3, FR-019/FR-075). 주어진 시간대 [{@code from}, {@code to})를 기준으로 각 회의실의 상태를
 * 계산한다.
 *
 * <p>상태는 회의실을 점유한 활성 회의로 판정한다: 사용 불가 회의실은 UNAVAILABLE, 진행 중(IN_PROGRESS) 회의가 겹치면 IN_USE, 예정(SCHEDULED)
 * 회의가 겹치면 RESERVED, 아무 회의도 겹치지 않으면 AVAILABLE이다. 별도 예약 테이블 없이 회의(Meeting) 데이터에서 직접 도출한다.
 */
@Service
public class GetMeetingRoomStatusUseCase {

    private final MeetingRoomRepositoryPort meetingRoomRepositoryPort;
    private final BuildingRepositoryPort buildingRepositoryPort;
    private final SiteRepositoryPort siteRepositoryPort;
    private final MeetingRepositoryPort meetingRepositoryPort;

    public GetMeetingRoomStatusUseCase(
            MeetingRoomRepositoryPort meetingRoomRepositoryPort,
            BuildingRepositoryPort buildingRepositoryPort,
            SiteRepositoryPort siteRepositoryPort,
            MeetingRepositoryPort meetingRepositoryPort) {
        this.meetingRoomRepositoryPort = meetingRoomRepositoryPort;
        this.buildingRepositoryPort = buildingRepositoryPort;
        this.siteRepositoryPort = siteRepositoryPort;
        this.meetingRepositoryPort = meetingRepositoryPort;
    }

    @Transactional(readOnly = true)
    public List<RoomStatusResult> execute(RoomStatusQuery query) {
        if (query.from() == null || query.to() == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "조회 시작/종료 시각은 필수입니다.");
        }
        if (!query.from().isBefore(query.to())) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "조회 시작 시각은 종료 시각보다 이전이어야 합니다.");
        }

        Map<UUID, Building> buildings = indexById(buildingRepositoryPort.findAll(), Building::id);
        Map<UUID, Site> sites = indexById(siteRepositoryPort.findAll(), Site::id);

        List<MeetingRoom> rooms =
                meetingRoomRepositoryPort.findAll().stream()
                        .filter(room -> matchesBuilding(room, query))
                        .filter(room -> matchesSite(room, query, buildings))
                        .sorted(Comparator.comparing(MeetingRoom::name))
                        .toList();

        List<UUID> roomIds = rooms.stream().map(MeetingRoom::id).toList();
        Map<UUID, List<Meeting>> overlapsByRoom =
                meetingRepositoryPort
                        .findActiveOverlapsInRooms(roomIds, query.from(), query.to())
                        .stream()
                        .collect(Collectors.groupingBy(Meeting::meetingRoomId));

        return rooms.stream()
                .map(
                        room ->
                                toStatus(
                                        room,
                                        buildings,
                                        sites,
                                        overlapsByRoom.getOrDefault(room.id(), List.of())))
                .toList();
    }

    private RoomStatusResult toStatus(
            MeetingRoom room,
            Map<UUID, Building> buildings,
            Map<UUID, Site> sites,
            List<Meeting> overlaps) {
        Building building = buildings.get(room.buildingId());
        Site site = building == null ? null : sites.get(building.siteId());

        RoomAvailabilityStatus status;
        Meeting occupying = null;
        if (!room.isAvailable()) {
            status = RoomAvailabilityStatus.UNAVAILABLE;
        } else {
            Meeting inProgress = earliest(overlaps, MeetingStatus.IN_PROGRESS);
            if (inProgress != null) {
                status = RoomAvailabilityStatus.IN_USE;
                occupying = inProgress;
            } else if (!overlaps.isEmpty()) {
                status = RoomAvailabilityStatus.RESERVED;
                occupying = earliestAny(overlaps);
            } else {
                status = RoomAvailabilityStatus.AVAILABLE;
            }
        }

        RoomStatusResult.OccupyingMeeting currentMeeting =
                occupying == null
                        ? null
                        : new RoomStatusResult.OccupyingMeeting(
                                occupying.id(),
                                occupying.scheduledAt(),
                                occupying.scheduledEndAt());

        return new RoomStatusResult(
                room.id(),
                room.name(),
                site == null ? null : site.id(),
                site == null ? null : site.name(),
                room.buildingId(),
                building == null ? null : building.name(),
                room.capacity(),
                status,
                currentMeeting);
    }

    private Meeting earliest(List<Meeting> meetings, MeetingStatus status) {
        return meetings.stream()
                .filter(meeting -> meeting.status() == status)
                .min(Comparator.comparing(Meeting::scheduledAt))
                .orElse(null);
    }

    private Meeting earliestAny(List<Meeting> meetings) {
        return meetings.stream().min(Comparator.comparing(Meeting::scheduledAt)).orElse(null);
    }

    private boolean matchesBuilding(MeetingRoom room, RoomStatusQuery query) {
        return query.buildingId() == null || query.buildingId().equals(room.buildingId());
    }

    private boolean matchesSite(
            MeetingRoom room, RoomStatusQuery query, Map<UUID, Building> buildings) {
        if (query.siteId() == null) {
            return true;
        }
        Building building = buildings.get(room.buildingId());
        return building != null && query.siteId().equals(building.siteId());
    }

    private static <T> Map<UUID, T> indexById(List<T> items, Function<T, UUID> idExtractor) {
        Map<UUID, T> index = new HashMap<>();
        for (T item : items) {
            index.put(idExtractor.apply(item), item);
        }
        return index;
    }
}