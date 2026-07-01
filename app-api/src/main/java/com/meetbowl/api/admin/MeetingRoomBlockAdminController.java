package com.meetbowl.api.admin;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.RequireAdmin;
import com.meetbowl.application.meetingroom.CreateRoomBlockCommand;
import com.meetbowl.application.meetingroom.RoomBlockAdminUseCase;
import com.meetbowl.application.meetingroom.RoomBlockResult;
import com.meetbowl.common.response.ApiResponse;

/** 관리자 회의실 시간대 차단 관리 API다. Admin 전용. 차단된 구간은 예약 가드가 신규 예약을 막는다. */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/admin/meeting-rooms/{roomId}/blocks")
@RequireAdmin
public class MeetingRoomBlockAdminController extends BaseController {

    private final RoomBlockAdminUseCase roomBlockAdminUseCase;

    public MeetingRoomBlockAdminController(RoomBlockAdminUseCase roomBlockAdminUseCase) {
        this.roomBlockAdminUseCase = roomBlockAdminUseCase;
    }

    /** 시간대 차단 등록. */
    @PostMapping
    public ResponseEntity<ApiResponse<RoomBlockResponse>> createBlock(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID roomId,
            @Valid @RequestBody CreateRoomBlockRequest request) {
        CreateRoomBlockCommand command =
                new CreateRoomBlockCommand(
                        roomId, request.startAt(), request.endAt(), request.reason());
        RoomBlockResult result = roomBlockAdminUseCase.create(command, admin.organizationId());
        return created(RoomBlockResponse.from(result));
    }

    /** 회의실의 차단 목록. */
    @GetMapping
    public ApiResponse<List<RoomBlockResponse>> listBlocks(@PathVariable UUID roomId) {
        List<RoomBlockResponse> blocks =
                roomBlockAdminUseCase.listByRoom(roomId).stream()
                        .map(RoomBlockResponse::from)
                        .toList();
        return ok(blocks);
    }

    /** 시간대 차단 해제. */
    @DeleteMapping("/{blockId}")
    public ApiResponse<Void> deleteBlock(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID roomId,
            @PathVariable UUID blockId) {
        roomBlockAdminUseCase.delete(blockId, admin.organizationId());
        return ok();
    }
}
