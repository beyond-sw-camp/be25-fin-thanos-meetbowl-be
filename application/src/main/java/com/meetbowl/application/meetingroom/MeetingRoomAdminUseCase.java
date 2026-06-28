package com.meetbowl.application.meetingroom;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meetingroom.Building;
import com.meetbowl.domain.meetingroom.BuildingRepositoryPort;
import com.meetbowl.domain.meetingroom.MeetingRoom;
import com.meetbowl.domain.meetingroom.MeetingRoomRepositoryPort;
import com.meetbowl.domain.meetingroom.Site;
import com.meetbowl.domain.meetingroom.SiteRepositoryPort;

/**
 * 관리자 회의실 기준정보 등록/수정/삭제/사용가능여부 변경 UseCase다(F2, FR-088).
 *
 * <p>회의실은 단순 CRUD라 액션별로 클래스를 쪼개기보다 하나의 admin UseCase로 묶는다(관리자 전용, 동일 도메인).
 */
@Service
public class MeetingRoomAdminUseCase {

    private final MeetingRoomRepositoryPort meetingRoomRepositoryPort;
    private final BuildingRepositoryPort buildingRepositoryPort;
    private final SiteRepositoryPort siteRepositoryPort;

    public MeetingRoomAdminUseCase(
            MeetingRoomRepositoryPort meetingRoomRepositoryPort,
            BuildingRepositoryPort buildingRepositoryPort,
            SiteRepositoryPort siteRepositoryPort) {
        this.meetingRoomRepositoryPort = meetingRoomRepositoryPort;
        this.buildingRepositoryPort = buildingRepositoryPort;
        this.siteRepositoryPort = siteRepositoryPort;
    }

    @Transactional
    public MeetingRoomResult create(CreateMeetingRoomCommand command) {
        return create(command, null);
    }

    @Transactional
    public MeetingRoomResult create(CreateMeetingRoomCommand command, UUID adminAffiliateId) {
        requireBuilding(command.buildingId());
        ensureBuildingAccessible(command.buildingId(), adminAffiliateId);
        MeetingRoom room =
                meetingRoomRepositoryPort.save(
                        MeetingRoom.of(
                                null,
                                command.buildingId(),
                                command.name(),
                                command.floor(),
                                command.location(),
                                command.capacity(),
                                command.available()));
        return MeetingRoomResult.of(room);
    }

    @Transactional
    public MeetingRoomResult update(UpdateMeetingRoomCommand command) {
        return update(command, null);
    }

    @Transactional
    public MeetingRoomResult update(UpdateMeetingRoomCommand command, UUID adminAffiliateId) {
        MeetingRoom room = requireRoom(command.roomId());
        requireBuilding(command.buildingId());
        ensureBuildingAccessible(room.buildingId(), adminAffiliateId);
        ensureBuildingAccessible(command.buildingId(), adminAffiliateId);
        MeetingRoom updated =
                meetingRoomRepositoryPort.save(
                        room.change(
                                command.buildingId(),
                                command.name(),
                                command.floor(),
                                command.location(),
                                command.capacity()));
        return MeetingRoomResult.of(updated);
    }

    @Transactional
    public MeetingRoomResult changeAvailability(UUID roomId, boolean available) {
        return changeAvailability(roomId, available, null);
    }

    @Transactional
    public MeetingRoomResult changeAvailability(
            UUID roomId, boolean available, UUID adminAffiliateId) {
        MeetingRoom room = requireRoom(roomId);
        ensureBuildingAccessible(room.buildingId(), adminAffiliateId);
        MeetingRoom updated = meetingRoomRepositoryPort.save(room.changeAvailability(available));
        return MeetingRoomResult.of(updated);
    }

    @Transactional
    public void delete(UUID roomId) {
        delete(roomId, null);
    }

    @Transactional
    public void delete(UUID roomId, UUID adminAffiliateId) {
        requireRoom(roomId);
        ensureBuildingAccessible(requireRoom(roomId).buildingId(), adminAffiliateId);
        meetingRoomRepositoryPort.deleteById(roomId);
    }

    private MeetingRoom requireRoom(UUID roomId) {
        return meetingRoomRepositoryPort
                .findById(roomId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "회의실을 찾을 수 없습니다."));
    }

    private void requireBuilding(UUID buildingId) {
        if (buildingRepositoryPort.findById(buildingId).isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "건물을 찾을 수 없습니다.");
        }
    }

    private void ensureBuildingAccessible(UUID buildingId, UUID adminAffiliateId) {
        if (adminAffiliateId == null) {
            return;
        }
        Building building =
                buildingRepositoryPort
                        .findById(buildingId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "건물을 찾을 수 없습니다."));
        Site site =
                siteRepositoryPort
                        .findById(building.siteId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "사이트를 찾을 수 없습니다."));
        if (!Objects.equals(site.affiliateId(), adminAffiliateId)) {
            throw new BusinessException(
                    ErrorCode.COMMON_FORBIDDEN, "다른 계열사의 회의실은 관리할 수 없습니다.");
        }
    }
}
