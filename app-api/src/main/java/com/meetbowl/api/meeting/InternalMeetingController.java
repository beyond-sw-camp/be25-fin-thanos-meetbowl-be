package com.meetbowl.api.meeting;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.api.meeting.dto.EndMeetingInternalRequest;
import com.meetbowl.api.meeting.dto.EndMeetingResponse;
import com.meetbowl.application.meeting.EndMeetingCommand;
import com.meetbowl.application.meeting.EndMeetingResult;
import com.meetbowl.application.meeting.EndMeetingUseCase;
import com.meetbowl.common.response.ApiResponse;

/**
 * 내부 시스템이 회의 종료 상태를 API 서버에 통지할 때 사용하는 controller다.
 *
 * <p>현재 주 용도는 STT 서버의 session stop 완료 알림이며, 외부 사용자가 직접 호출하는 공개 API가 아니다.
 */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/internal/meetings")
public class InternalMeetingController extends BaseController {

    private final GlobalPermissionChecker permissionChecker;
    private final EndMeetingUseCase endMeetingUseCase;

    public InternalMeetingController(
            GlobalPermissionChecker permissionChecker, EndMeetingUseCase endMeetingUseCase) {
        this.permissionChecker = permissionChecker;
        this.endMeetingUseCase = endMeetingUseCase;
    }

    @PostMapping("/{meetingId}/end")
    public ApiResponse<EndMeetingResponse> endMeeting(
            @PathVariable UUID meetingId,
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody EndMeetingInternalRequest request) {
        permissionChecker.requireSystem(currentUser);

        EndMeetingResult result =
                endMeetingUseCase.execute(
                        new EndMeetingCommand(
                                meetingId,
                                request.endedAt(),
                                request.correlationId(),
                                request.reason(),
                                request.triggeredBy()));
        return ok(EndMeetingResponse.from(result));
    }
}
