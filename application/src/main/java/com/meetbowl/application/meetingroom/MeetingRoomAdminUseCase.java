package com.meetbowl.application.meetingroom;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meetingroom.BuildingRepositoryPort;
import com.meetbowl.domain.meetingroom.MeetingRoom;
import com.meetbowl.domain.meetingroom.MeetingRoomRepositoryPort;

/**
 * 관리자 회의실 기준정보 등록/수정/삭제/사용가능여부 변경 UseCase다(F2, FR-088).
 *
 * <p>회의실은 단순 CRUD라 액션별로 클래스를 쪼개기보다 하나의 admin UseCase로 묶는다(관리자 전용, 동일 도메인).
 */
@Service
public class MeetingRoomAdminUseCase {

    private final MeetingRoomRepositoryPort meetingRoomRepositoryPort;
    private final BuildingRepositoryPort buildingRepositoryPort;

    public MeetingRoomAdminUseCase(
            MeetingRoomRepositoryPort meetingRoomRepositoryPort,
            BuildingRepositoryPort buildingRepositoryPort) {
        this.meetingRoomRepositoryPort = meetingRoomRepositoryPort;
        this.buildingRepositoryPort = buildingRepositoryPort;
    }

    @Transactional
    public MeetingRoomResult create(CreateMeetingRoomCommand command) {
        requireBuilding(command.buildingId());
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
        MeetingRoom room = requireRoom(command.roomId());
        requireBuilding(command.buildingId());
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
        MeetingRoom room = requireRoom(roomId);
        MeetingRoom updated = meetingRoomRepositoryPort.save(room.changeAvailability(available));
        return MeetingRoomResult.of(updated);
    }

    @Transactional
    public void delete(UUID roomId) {
        requireRoom(roomId);
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
}