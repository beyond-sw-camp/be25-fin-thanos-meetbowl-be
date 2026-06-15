package com.meetbowl.api.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
import com.meetbowl.api.common.auth.RequireUserOrAdmin;
import com.meetbowl.application.meeting.CancelMeetingUseCase;
import com.meetbowl.application.meeting.CreateMeetingCommand;
import com.meetbowl.application.meeting.CreateMeetingUseCase;
import com.meetbowl.application.meeting.GetMeetingUseCase;
import com.meetbowl.application.meeting.MeetingListFilter;
import com.meetbowl.application.meeting.MeetingResult;
import com.meetbowl.application.meeting.UpdateMeetingCommand;
import com.meetbowl.application.meeting.UpdateMeetingUseCase;
import com.meetbowl.common.response.ApiResponse;

/** 회의 생성·조회·수정·취소 API다. 회의실을 사용하는 회의는 같은 회의실·겹치는 시간대의 중복 예약을 막는다. */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/meetings")
@RequireUserOrAdmin
public class MeetingController extends BaseController {

    private final CreateMeetingUseCase createMeetingUseCase;
    private final GetMeetingUseCase getMeetingUseCase;
    private final UpdateMeetingUseCase updateMeetingUseCase;
    private final CancelMeetingUseCase cancelMeetingUseCase;

    public MeetingController(
            CreateMeetingUseCase createMeetingUseCase,
            GetMeetingUseCase getMeetingUseCase,
            UpdateMeetingUseCase updateMeetingUseCase,
            CancelMeetingUseCase cancelMeetingUseCase) {
        this.createMeetingUseCase = createMeetingUseCase;
        this.getMeetingUseCase = getMeetingUseCase;
        this.updateMeetingUseCase = updateMeetingUseCase;
        this.cancelMeetingUseCase = cancelMeetingUseCase;
    }

    /** 회의 생성. 주최자는 인증 토큰에서 가져온다. 회의실 시간대가 겹치면 409(MEETING_ROOM_ALREADY_RESERVED). */
    @PostMapping
    public ResponseEntity<ApiResponse<MeetingResponse>> createMeeting(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody CreateMeetingRequest request) {
        CreateMeetingCommand command =
                new CreateMeetingCommand(
                        request.title(),
                        request.scheduledAt(),
                        request.scheduledEndAt(),
                        currentUser.userId(),
                        request.meetingRoomId(),
                        request.provider(),
                        request.providerRoomId(),
                        request.attendeeUserIds(),
                        request.reviewerUserId(),
                        request.description());
        MeetingResult result = createMeetingUseCase.execute(command);
        return created(MeetingResponse.from(result));
    }

    /**
     * 내 회의 목록 조회. {@code role}로 전체(all)/주최(host)/초대(invited)를 구분하고, {@code from}/{@code to}로 예정 시작
     * 시각 범위를 거른다. FE의 "전체 / 내가 주최한 회의 / 초대된 회의" 탭에 대응한다.
     */
    @GetMapping
    public ApiResponse<List<MeetingResponse>> getMyMeetings(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "all") String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to) {
        List<MeetingResponse> responses =
                getMeetingUseCase
                        .getMyMeetings(currentUser.userId(), toFilter(role), from, to)
                        .stream()
                        .map(MeetingResponse::from)
                        .toList();
        return ok(responses);
    }

    /** 회의 단건 조회(상세). 주최자/참석자/Admin만 조회 가능. 회의 내용(description)은 이 상세 응답에서만 노출한다. */
    @GetMapping("/{meetingId}")
    public ApiResponse<MeetingDetailResponse> getMeeting(
            @CurrentUser AuthenticatedUser currentUser, @PathVariable UUID meetingId) {
        MeetingResult result =
                getMeetingUseCase.getById(meetingId, currentUser.userId(), currentUser.isAdmin());
        return ok(MeetingDetailResponse.from(result));
    }

    /** 회의 수정. 주최자만 가능. 회의실 시간대가 겹치면 409(MEETING_ROOM_ALREADY_RESERVED). */
    @PatchMapping("/{meetingId}")
    public ApiResponse<MeetingResponse> updateMeeting(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID meetingId,
            @Valid @RequestBody UpdateMeetingRequest request) {
        UpdateMeetingCommand command =
                new UpdateMeetingCommand(
                        meetingId,
                        currentUser.userId(),
                        request.title(),
                        request.scheduledAt(),
                        request.scheduledEndAt(),
                        request.meetingRoomId(),
                        request.description());
        return ok(MeetingResponse.from(updateMeetingUseCase.execute(command)));
    }

    /** 회의 취소. 주최자만 가능. */
    @PostMapping("/{meetingId}/cancel")
    public ApiResponse<MeetingResponse> cancelMeeting(
            @CurrentUser AuthenticatedUser currentUser, @PathVariable UUID meetingId) {
        MeetingResult result = cancelMeetingUseCase.execute(meetingId, currentUser.userId());
        return ok(MeetingResponse.from(result));
    }

    private MeetingListFilter toFilter(String role) {
        return switch (role == null ? "all" : role.toLowerCase()) {
            case "host" -> MeetingListFilter.HOST;
            case "invited" -> MeetingListFilter.INVITED;
            default -> MeetingListFilter.ALL;
        };
    }
}
