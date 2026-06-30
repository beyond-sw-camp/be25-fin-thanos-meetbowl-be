package com.meetbowl.application.meetingroom;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.meetingroom.Building;
import com.meetbowl.domain.meetingroom.BuildingRepositoryPort;
import com.meetbowl.domain.meetingroom.MeetingRoom;
import com.meetbowl.domain.meetingroom.MeetingRoomRepositoryPort;
import com.meetbowl.domain.meetingroom.RoomBlock;
import com.meetbowl.domain.meetingroom.RoomBlockRepositoryPort;
import com.meetbowl.domain.meetingroom.Site;
import com.meetbowl.domain.meetingroom.SiteRepositoryPort;

/**
 * 관리자 회의실 시간대 차단 등록/삭제/조회 UseCase다. 차단된 구간 {@code [startAt, endAt)}은 {@link
 * com.meetbowl.application.meeting.MeetingRoomReservationGuard}가 신규 예약을 막는다. 차단 등록 시점에 이미 활성 예약이 겹쳐 있으면 등록을
 * 거부한다(점검 시간에 회의가 남지 않도록). 회의실 행에 비관적 잠금을 걸어 동시 예약 요청과 직렬화한다.
 *
 * <p>회의실 CRUD와 동일하게 {@code adminAffiliateId}가 주어지면 다른 계열사 회의실은 관리하지 못하도록 막는다(null이면 검사 생략).
 */
@Service
public class RoomBlockAdminUseCase {

    private final RoomBlockRepositoryPort roomBlockRepositoryPort;
    private final MeetingRoomRepositoryPort meetingRoomRepositoryPort;
    private final MeetingRepositoryPort meetingRepositoryPort;
    private final BuildingRepositoryPort buildingRepositoryPort;
    private final SiteRepositoryPort siteRepositoryPort;

    public RoomBlockAdminUseCase(
            RoomBlockRepositoryPort roomBlockRepositoryPort,
            MeetingRoomRepositoryPort meetingRoomRepositoryPort,
            MeetingRepositoryPort meetingRepositoryPort,
            BuildingRepositoryPort buildingRepositoryPort,
            SiteRepositoryPort siteRepositoryPort) {
        this.roomBlockRepositoryPort = roomBlockRepositoryPort;
        this.meetingRoomRepositoryPort = meetingRoomRepositoryPort;
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.buildingRepositoryPort = buildingRepositoryPort;
        this.siteRepositoryPort = siteRepositoryPort;
    }

    @Transactional
    public RoomBlockResult create(CreateRoomBlockCommand command) {
        return create(command, null);
    }

    @Transactional
    public RoomBlockResult create(CreateRoomBlockCommand command, UUID adminAffiliateId) {
        // 회의실 행에 비관적 쓰기 잠금을 걸어 동시 예약 요청과 직렬화한다(차단~예약 경합 방지).
        MeetingRoom room =
                meetingRoomRepositoryPort
                        .findByIdForUpdate(command.roomId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "회의실을 찾을 수 없습니다."));
        ensureRoomAccessible(room, adminAffiliateId);

        // 구간 유효성(종료 > 시작 등)은 RoomBlock.create에서 검증한다.
        RoomBlock block =
                RoomBlock.create(
                        command.roomId(), command.startAt(), command.endAt(), command.reason());

        // 이미 활성 예약(SCHEDULED/IN_PROGRESS)이 겹치면 사용 제한을 걸 수 없다(점검 시간에 회의가 남지 않도록).
        boolean reservationExists =
                !meetingRepositoryPort
                        .findActiveRoomOverlaps(command.roomId(), block.startAt(), block.endAt())
                        .isEmpty();
        if (reservationExists) {
            throw new BusinessException(
                    ErrorCode.MEETING_ROOM_ALREADY_RESERVED,
                    "해당 시간대에 이미 예약이 있어 사용 제한할 수 없습니다.");
        }

        return RoomBlockResult.of(roomBlockRepositoryPort.save(block));
    }

    @Transactional(readOnly = true)
    public List<RoomBlockResult> listByRoom(UUID roomId) {
        requireRoom(roomId);
        return roomBlockRepositoryPort.findByRoomId(roomId).stream()
                .map(RoomBlockResult::of)
                .toList();
    }

    @Transactional
    public void delete(UUID blockId) {
        delete(blockId, null);
    }

    @Transactional
    public void delete(UUID blockId, UUID adminAffiliateId) {
        RoomBlock block =
                roomBlockRepositoryPort
                        .findById(blockId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "차단을 찾을 수 없습니다."));
        ensureRoomAccessible(requireRoom(block.roomId()), adminAffiliateId);
        roomBlockRepositoryPort.deleteById(blockId);
    }

    private MeetingRoom requireRoom(UUID roomId) {
        return meetingRoomRepositoryPort
                .findById(roomId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "회의실을 찾을 수 없습니다."));
    }

    private void ensureRoomAccessible(MeetingRoom room, UUID adminAffiliateId) {
        if (adminAffiliateId == null) {
            return;
        }
        Building building =
                buildingRepositoryPort
                        .findById(room.buildingId())
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
