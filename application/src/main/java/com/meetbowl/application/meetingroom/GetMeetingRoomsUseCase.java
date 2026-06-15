package com.meetbowl.application.meetingroom;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.response.PageResponse;
import com.meetbowl.domain.meetingroom.Building;
import com.meetbowl.domain.meetingroom.BuildingRepositoryPort;
import com.meetbowl.domain.meetingroom.MeetingRoom;
import com.meetbowl.domain.meetingroom.MeetingRoomRepositoryPort;
import com.meetbowl.domain.meetingroom.Site;
import com.meetbowl.domain.meetingroom.SiteRepositoryPort;

/**
 * 회의실 목록 조회 UseCase다(F3, FR-019). 사이트/건물/수용인원 필터를 적용해 목록을 반환한다.
 *
 * <p>회의실 기준정보는 건수가 적어 필터/조인/페이지를 application 메모리에서 처리한다. 데이터가 커지면 DB 조회로 최적화한다(NFR-010).
 */
@Service
public class GetMeetingRoomsUseCase {

    private final MeetingRoomRepositoryPort meetingRoomRepositoryPort;
    private final BuildingRepositoryPort buildingRepositoryPort;
    private final SiteRepositoryPort siteRepositoryPort;

    public GetMeetingRoomsUseCase(
            MeetingRoomRepositoryPort meetingRoomRepositoryPort,
            BuildingRepositoryPort buildingRepositoryPort,
            SiteRepositoryPort siteRepositoryPort) {
        this.meetingRoomRepositoryPort = meetingRoomRepositoryPort;
        this.buildingRepositoryPort = buildingRepositoryPort;
        this.siteRepositoryPort = siteRepositoryPort;
    }

    @Transactional(readOnly = true)
    public PageResponse<MeetingRoomListItemResult> execute(GetMeetingRoomsQuery query) {
        Map<UUID, Building> buildings = indexById(buildingRepositoryPort.findAll(), Building::id);
        Map<UUID, Site> sites = indexById(siteRepositoryPort.findAll(), Site::id);

        List<MeetingRoom> filtered =
                meetingRoomRepositoryPort.findAll().stream()
                        .filter(room -> matchesBuilding(room, query))
                        .filter(room -> matchesSite(room, query, buildings))
                        .filter(
                                room ->
                                        query.minCapacity() == null
                                                || room.capacity() >= query.minCapacity())
                        .sorted(Comparator.comparing(MeetingRoom::name))
                        .toList();

        long totalElements = filtered.size();
        List<MeetingRoomListItemResult> items =
                paginate(filtered, query.page(), query.size()).stream()
                        .map(room -> toItem(room, buildings, sites))
                        .toList();
        return PageResponse.of(items, query.page(), query.size(), totalElements);
    }

    private boolean matchesBuilding(MeetingRoom room, GetMeetingRoomsQuery query) {
        return query.buildingId() == null || query.buildingId().equals(room.buildingId());
    }

    private boolean matchesSite(
            MeetingRoom room, GetMeetingRoomsQuery query, Map<UUID, Building> buildings) {
        if (query.siteId() == null) {
            return true;
        }
        Building building = buildings.get(room.buildingId());
        return building != null && query.siteId().equals(building.siteId());
    }

    private MeetingRoomListItemResult toItem(
            MeetingRoom room, Map<UUID, Building> buildings, Map<UUID, Site> sites) {
        Building building = buildings.get(room.buildingId());
        Site site = building == null ? null : sites.get(building.siteId());
        return MeetingRoomListItemResult.of(room, building, site);
    }

    private static <T> Map<UUID, T> indexById(List<T> items, Function<T, UUID> idExtractor) {
        Map<UUID, T> index = new HashMap<>();
        for (T item : items) {
            index.put(idExtractor.apply(item), item);
        }
        return index;
    }

    private static <T> List<T> paginate(List<T> items, int page, int size) {
        int fromIndex = Math.min((page - 1) * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }
}
