package com.meetbowl.api.admin;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import com.meetbowl.application.meetingroom.CreateMeetingRoomCommand;
import com.meetbowl.application.meetingroom.MeetingRoomAdminUseCase;
import com.meetbowl.application.meetingroom.MeetingRoomResult;
import com.meetbowl.application.meetingroom.UpdateMeetingRoomCommand;
import com.meetbowl.common.response.ApiResponse;

/** 관리자 회의실 기준정보 관리 API다(F2, FR-088). Admin 전용. */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/admin/meeting-rooms")
@RequireAdmin
public class MeetingRoomAdminController extends BaseController {

    private final MeetingRoomAdminUseCase meetingRoomAdminUseCase;

    public MeetingRoomAdminController(MeetingRoomAdminUseCase meetingRoomAdminUseCase) {
        this.meetingRoomAdminUseCase = meetingRoomAdminUseCase;
    }

    /** 회의실 등록. */
    @PostMapping
    public ResponseEntity<ApiResponse<MeetingRoomResponse>> createMeetingRoom(
            @CurrentUser AuthenticatedUser admin,
            @Valid @RequestBody CreateMeetingRoomRequest request) {
        CreateMeetingRoomCommand command =
                new CreateMeetingRoomCommand(
                        request.buildingId(),
                        request.name(),
                        request.floor(),
                        request.location(),
                        request.capacity(),
                        request.isAvailable() == null || request.isAvailable());
        MeetingRoomResult result =
                meetingRoomAdminUseCase.create(command, admin.organizationId());
        return created(MeetingRoomResponse.from(result));
    }

    /** 회의실 수정. */
    @PatchMapping("/{roomId}")
    public ApiResponse<MeetingRoomResponse> updateMeetingRoom(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID roomId,
            @Valid @RequestBody UpdateMeetingRoomRequest request) {
        UpdateMeetingRoomCommand command =
                new UpdateMeetingRoomCommand(
                        roomId,
                        request.buildingId(),
                        request.name(),
                        request.floor(),
                        request.location(),
                        request.capacity());
        return ok(
                MeetingRoomResponse.from(
                        meetingRoomAdminUseCase.update(command, admin.organizationId())));
    }

    /** 회의실 사용 가능 여부 변경(FR-089). */
    @PatchMapping("/{roomId}/availability")
    public ApiResponse<MeetingRoomResponse> changeAvailability(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID roomId,
            @Valid @RequestBody ChangeRoomAvailabilityRequest request) {
        MeetingRoomResult result =
                meetingRoomAdminUseCase.changeAvailability(
                        roomId, request.isAvailable(), admin.organizationId());
        return ok(MeetingRoomResponse.from(result));
    }

    /** 회의실 삭제. */
    @DeleteMapping("/{roomId}")
    public ApiResponse<Void> deleteMeetingRoom(
            @CurrentUser AuthenticatedUser admin, @PathVariable UUID roomId) {
        meetingRoomAdminUseCase.delete(roomId, admin.organizationId());
        return ok();
    }
}
