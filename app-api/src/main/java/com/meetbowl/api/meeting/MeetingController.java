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
import com.meetbowl.api.meeting.dto.JoinMeetingRequest;
import com.meetbowl.api.meeting.dto.JoinMeetingResponse;
import com.meetbowl.application.meeting.JoinMeetingCommand;
import com.meetbowl.application.meeting.JoinMeetingResult;
import com.meetbowl.application.meeting.JoinMeetingUseCase;
import com.meetbowl.common.response.ApiResponse;

/**
 * 회의 관련 API다.
 *
 * <p>이번 join API의 핵심은 "프론트가 LiveKit secret을 몰라도 된다"는 점이다. 따라서 Controller는 회의 참여자 정보를 받아 UseCase에 넘기고,
 * LiveKit JWT 자체는 절대 응답 외 다른 형태로 노출하지 않는다.
 */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/meetings")
public class MeetingController extends BaseController {

    private final JoinMeetingUseCase joinMeetingUseCase;

    public MeetingController(JoinMeetingUseCase joinMeetingUseCase) {
        this.joinMeetingUseCase = joinMeetingUseCase;
    }

    @PostMapping("/{meetingId}/join")
    public ApiResponse<JoinMeetingResponse> joinMeeting(
            @PathVariable UUID meetingId,
            @CurrentUser(required = false) AuthenticatedUser currentUser,
            @Valid @RequestBody JoinMeetingRequest request) {
        JoinMeetingResult result =
                joinMeetingUseCase.execute(
                        new JoinMeetingCommand(
                                meetingId,
                                currentUser != null ? currentUser.userId() : null,
                                request.displayName(),
                                request.participantIdentity()));

        return ok(JoinMeetingResponse.from(result));
    }
}
