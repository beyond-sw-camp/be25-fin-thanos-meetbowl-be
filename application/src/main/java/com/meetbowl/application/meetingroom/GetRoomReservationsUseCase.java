package com.meetbowl.application.meetingroom;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.application.meeting.MeetingListFilter;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.meeting.MeetingStatus;
import com.meetbowl.domain.meetingroom.Building;
import com.meetbowl.domain.meetingroom.BuildingRepositoryPort;
import com.meetbowl.domain.meetingroom.MeetingRoom;
import com.meetbowl.domain.meetingroom.MeetingRoomRepositoryPort;
import com.meetbowl.domain.meetingroom.Site;
import com.meetbowl.domain.meetingroom.SiteRepositoryPort;

/**
 * 회의실 예약 현황 조회 UseCase다(F4). "예약"은 회의실을 점유한 회의(Meeting)이며, 이 UseCase는 회의실 도메인 관점으로 노출한다.
 *
 * <ul>
 *   <li>{@link #getMyReservations} — 내가 예약(주최)했거나 참석할 회의실 회의 목록(화면 ①②). 회의실 없는 화상회의는 제외한다.
 *   <li>{@link #getReservationBoard} — 조회 시간대에 회의실별로 잡힌 예약 블록 전체(타임라인 ③). 모든 사용자의 예약을 포함한다.
 * </ul>
 */
@Service
public class GetRoomReservationsUseCase {

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort;
    private final MeetingRoomRepositoryPort meetingRoomRepositoryPort;
    private final BuildingRepositoryPort buildingRepositoryPort;
    private final SiteRepositoryPort siteRepositoryPort;

    public GetRoomReservationsUseCase(
            MeetingRepositoryPort meetingRepositoryPort,
            MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort,
            MeetingRoomRepositoryPort meetingRoomRepositoryPort,
            BuildingRepositoryPort buildingRepositoryPort,
            SiteRepositoryPort siteRepositoryPort) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.meetingAttendeeRepositoryPort = meetingAttendeeRepositoryPort;
        this.meetingRoomRepositoryPort = meetingRoomRepositoryPort;
        this.buildingRepositoryPort = buildingRepositoryPort;
        this.siteRepositoryPort = siteRepositoryPort;
    }

    /**
     * 내 회의실 예약 목록(①②). {@code filter}로 주최(host)/참석(invited)/전체(all)를 구분한다.
     * 활성(SCHEDULED/IN_PROGRESS)만 본다.
     */
    @Transactional(readOnly = true)
    public List<ReservationItemResult> getMyReservations(UUID userId, MeetingListFilter filter) {
        return getMyReservations(userId, filter, null);
    }

    @Transactional(readOnly = true)
    public List<ReservationItemResult> getMyReservations(
            UUID userId, MeetingListFilter filter, UUID affiliateId) {
        List<Meeting> meetings =
                switch (filter) {
                    case HOST -> meetingRepositoryPort.findByHostUserId(userId);
                    case INVITED -> invitedMeetings(userId);
                    case ALL ->
                            union(
                                    meetingRepositoryPort.findByHostUserId(userId),
                                    invitedMeetings(userId));
                };

        Map<UUID, MeetingRoom> rooms =
                indexById(meetingRoomRepositoryPort.findAll(), MeetingRoom::id);
        Map<UUID, Building> buildings = indexById(buildingRepositoryPort.findAll(), Building::id);
        Map<UUID, Site> sites = indexById(siteRepositoryPort.findAll(), Site::id);

        return meetings.stream()
                .filter(meeting -> meeting.meetingRoomId() != null)
                .filter(this::isActive)
                .filter(meeting -> matchesAffiliate(meeting, affiliateId, rooms, buildings, sites))
                .sorted(Comparator.comparing(Meeting::scheduledAt))
                .map(meeting -> toItem(meeting, rooms))
                .toList();
    }

    /** 회의실 예약 타임라인(③). 모든 사용자의 예약 블록을 회의실별로 묶어 반환한다. */
    @Transactional(readOnly = true)
    public List<RoomReservationsResult> getReservationBoard(
            Instant from, Instant to, UUID siteId, UUID buildingId) {
        return getReservationBoard(from, to, siteId, buildingId, null);
    }

    @Transactional(readOnly = true)
    public List<RoomReservationsResult> getReservationBoard(
            Instant from, Instant to, UUID siteId, UUID buildingId, UUID affiliateId) {
        if (from == null || to == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "조회 시작/종료 시각은 필수입니다.");
        }
        if (!from.isBefore(to)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "조회 시작 시각은 종료 시각보다 이전이어야 합니다.");
        }

        Map<UUID, Building> buildings = indexById(buildingRepositoryPort.findAll(), Building::id);
        Map<UUID, Site> sites = indexById(siteRepositoryPort.findAll(), Site::id);

        List<MeetingRoom> rooms =
                meetingRoomRepositoryPort.findAll().stream()
                        .filter(room -> matchesAffiliate(room, affiliateId, buildings, sites))
                        .filter(room -> buildingId == null || buildingId.equals(room.buildingId()))
                        .filter(room -> matchesSite(room, siteId, buildings))
                        .sorted(Comparator.comparing(MeetingRoom::name))
                        .toList();

        List<UUID> roomIds = rooms.stream().map(MeetingRoom::id).toList();
        Map<UUID, List<Meeting>> byRoom =
                meetingRepositoryPort.findActiveOverlapsInRooms(roomIds, from, to).stream()
                        .collect(Collectors.groupingBy(Meeting::meetingRoomId));

        return rooms.stream()
                .map(
                        room ->
                                toRoomReservations(
                                        room,
                                        buildings,
                                        sites,
                                        byRoom.getOrDefault(room.id(), List.of())))
                .toList();
    }

    private RoomReservationsResult toRoomReservations(
            MeetingRoom room,
            Map<UUID, Building> buildings,
            Map<UUID, Site> sites,
            List<Meeting> meetings) {
        Building building = buildings.get(room.buildingId());
        Site site = building == null ? null : sites.get(building.siteId());

        List<RoomReservationsResult.Reservation> reservations =
                meetings.stream()
                        .sorted(Comparator.comparing(Meeting::scheduledAt))
                        .map(
                                meeting ->
                                        new RoomReservationsResult.Reservation(
                                                meeting.id(),
                                                meeting.title(),
                                                meeting.scheduledAt(),
                                                meeting.scheduledEndAt(),
                                                meeting.hostUserId()))
                        .toList();

        return new RoomReservationsResult(
                room.id(),
                room.name(),
                room.capacity(),
                room.isAvailable(),
                site == null ? null : site.id(),
                site == null ? null : site.name(),
                room.buildingId(),
                building == null ? null : building.name(),
                reservations);
    }

    private ReservationItemResult toItem(Meeting meeting, Map<UUID, MeetingRoom> rooms) {
        MeetingRoom room = rooms.get(meeting.meetingRoomId());
        return new ReservationItemResult(
                meeting.id(),
                meeting.meetingRoomId(),
                room == null ? null : room.name(),
                meeting.title(),
                meeting.scheduledAt(),
                meeting.scheduledEndAt(),
                meeting.hostUserId());
    }

    /** 내가 참석자(주최 제외)로 초대된 회의. */
    private List<Meeting> invitedMeetings(UUID userId) {
        List<Meeting> meetings = new ArrayList<>();
        meetingAttendeeRepositoryPort.findByUserId(userId).stream()
                .map(MeetingAttendee::meetingId)
                .distinct()
                .forEach(
                        meetingId ->
                                meetingRepositoryPort
                                        .findById(meetingId)
                                        .filter(meeting -> !meeting.isHostedBy(userId))
                                        .ifPresent(meetings::add));
        return meetings;
    }

    private List<Meeting> union(List<Meeting> hosted, List<Meeting> invited) {
        Map<UUID, Meeting> byId = new LinkedHashMap<>();
        hosted.forEach(meeting -> byId.put(meeting.id(), meeting));
        invited.forEach(meeting -> byId.putIfAbsent(meeting.id(), meeting));
        return new ArrayList<>(byId.values());
    }

    private boolean isActive(Meeting meeting) {
        return meeting.status() == MeetingStatus.SCHEDULED
                || meeting.status() == MeetingStatus.IN_PROGRESS;
    }

    private boolean matchesSite(MeetingRoom room, UUID siteId, Map<UUID, Building> buildings) {
        if (siteId == null) {
            return true;
        }
        Building building = buildings.get(room.buildingId());
        return building != null && siteId.equals(building.siteId());
    }

    private boolean matchesAffiliate(
            MeetingRoom room,
            UUID affiliateId,
            Map<UUID, Building> buildings,
            Map<UUID, Site> sites) {
        if (affiliateId == null) {
            return true;
        }
        Building building = buildings.get(room.buildingId());
        Site site = building == null ? null : sites.get(building.siteId());
        return site != null && Objects.equals(site.affiliateId(), affiliateId);
    }

    private boolean matchesAffiliate(
            Meeting meeting,
            UUID affiliateId,
            Map<UUID, MeetingRoom> rooms,
            Map<UUID, Building> buildings,
            Map<UUID, Site> sites) {
        if (affiliateId == null) {
            return true;
        }
        MeetingRoom room = rooms.get(meeting.meetingRoomId());
        if (room == null) {
            return false;
        }
        return matchesAffiliate(room, affiliateId, buildings, sites);
    }

    private static <T> Map<UUID, T> indexById(List<T> items, Function<T, UUID> idExtractor) {
        Map<UUID, T> index = new HashMap<>();
        for (T item : items) {
            index.put(idExtractor.apply(item), item);
        }
        return index;
    }
}
