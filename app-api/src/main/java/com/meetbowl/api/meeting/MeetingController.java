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
import com.meetbowl.api.common.auth.RequireUserOrAdmin;
import com.meetbowl.api.meeting.dto.EndMeetingResponse;
import com.meetbowl.api.meeting.dto.JoinMeetingRequest;
import com.meetbowl.api.meeting.dto.JoinMeetingResponse;
import com.meetbowl.api.meeting.dto.TransferMeetingHostRequest;
import com.meetbowl.application.meeting.CancelMeetingUseCase;
import com.meetbowl.application.meeting.CheckAttendeeAvailabilityUseCase;
import com.meetbowl.application.meeting.CreateMeetingCommand;
import com.meetbowl.application.meeting.CreateMeetingUseCase;
import com.meetbowl.application.meeting.EndMeetingCommand;
import com.meetbowl.application.meeting.EndMeetingResult;
import com.meetbowl.application.meeting.EndMeetingUseCase;
import com.meetbowl.application.meeting.GetMeetingUseCase;
import com.meetbowl.application.meeting.JoinMeetingCommand;
import com.meetbowl.application.meeting.JoinMeetingResult;
import com.meetbowl.application.meeting.JoinMeetingUseCase;
import com.meetbowl.application.meeting.MeetingListFilter;
import com.meetbowl.application.meeting.MeetingResult;
import com.meetbowl.application.meeting.StartMeetingUseCase;
import com.meetbowl.application.meeting.TransferMeetingHostCommand;
import com.meetbowl.application.meeting.TransferMeetingHostUseCase;
import com.meetbowl.application.meeting.UpdateMeetingCommand;
import com.meetbowl.application.meeting.UpdateMeetingUseCase;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.common.response.ApiResponse;

/** 회의 생성, 조회, 수정, 취소와 회의 입장 정보를 제공하는 API다. */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/meetings")
public class MeetingController extends BaseController {

    private final CreateMeetingUseCase createMeetingUseCase;
    private final GetMeetingUseCase getMeetingUseCase;
    private final UpdateMeetingUseCase updateMeetingUseCase;
    private final CancelMeetingUseCase cancelMeetingUseCase;
    private final EndMeetingUseCase endMeetingUseCase;
    private final StartMeetingUseCase startMeetingUseCase;
    private final TransferMeetingHostUseCase transferMeetingHostUseCase;
    private final JoinMeetingUseCase joinMeetingUseCase;
    private final CheckAttendeeAvailabilityUseCase checkAttendeeAvailabilityUseCase;

    public MeetingController(
            CreateMeetingUseCase createMeetingUseCase,
            GetMeetingUseCase getMeetingUseCase,
            UpdateMeetingUseCase updateMeetingUseCase,
            CancelMeetingUseCase cancelMeetingUseCase,
            EndMeetingUseCase endMeetingUseCase,
            StartMeetingUseCase startMeetingUseCase,
            TransferMeetingHostUseCase transferMeetingHostUseCase,
            JoinMeetingUseCase joinMeetingUseCase,
            CheckAttendeeAvailabilityUseCase checkAttendeeAvailabilityUseCase) {
        this.createMeetingUseCase = createMeetingUseCase;
        this.getMeetingUseCase = getMeetingUseCase;
        this.updateMeetingUseCase = updateMeetingUseCase;
        this.cancelMeetingUseCase = cancelMeetingUseCase;
        this.endMeetingUseCase = endMeetingUseCase;
        this.startMeetingUseCase = startMeetingUseCase;
        this.transferMeetingHostUseCase = transferMeetingHostUseCase;
        this.joinMeetingUseCase = joinMeetingUseCase;
        this.checkAttendeeAvailabilityUseCase = checkAttendeeAvailabilityUseCase;
    }

    /** 회의 생성. 회의실 시간이 겹치면 409(MEETING_ROOM_ALREADY_RESERVED)를 반환한다. */
    @PostMapping
    @RequireUserOrAdmin
    public ResponseEntity<ApiResponse<MeetingResponse>> createMeeting(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody CreateMeetingRequest request) {
        CreateMeetingCommand command =
                new CreateMeetingCommand(
                        request.title(),
                        request.scheduledAt(),
                        request.scheduledEndAt(),
                        currentUser.userId(),
                        currentUser.organizationId(),
                        request.meetingRoomId(),
                        request.provider(),
                        request.providerRoomId(),
                        request.attendeeUserIds(),
                        request.externalInvitees() == null
                                ? List.of()
                                : request.externalInvitees().stream()
                                        .map(ExternalInviteeRequest::toCommand)
                                        .toList(),
                        request.reviewerUserId(),
                        request.description());
        MeetingResult result = createMeetingUseCase.execute(command);
        return created(MeetingResponse.from(result));
    }

    /**
     * 내 회의 목록 조회.
     *
     * <p>{@code role}로 전체(all), 주최(host), 초대(invited)를 구분하고 기간 조건으로 시작 시간을 필터링한다.
     */
    @GetMapping
    @RequireUserOrAdmin
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

    /** 회의 단건 상세 조회. */
    @GetMapping("/{meetingId}")
    @RequireUserOrAdmin
    public ApiResponse<MeetingDetailResponse> getMeeting(
            @CurrentUser AuthenticatedUser currentUser, @PathVariable UUID meetingId) {
        MeetingResult result =
                getMeetingUseCase.getById(meetingId, currentUser.userId(), currentUser.isAdmin());
        return ok(MeetingDetailResponse.from(result));
    }

    /** 회의 수정. 회의실 시간이 겹치면 409(MEETING_ROOM_ALREADY_RESERVED)를 반환한다. */
    @PatchMapping("/{meetingId}")
    @RequireUserOrAdmin
    public ApiResponse<MeetingResponse> updateMeeting(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID meetingId,
            @Valid @RequestBody UpdateMeetingRequest request) {
        UpdateMeetingCommand command =
                new UpdateMeetingCommand(
                        meetingId,
                        currentUser.userId(),
                        currentUser.organizationId(),
                        request.title(),
                        request.scheduledAt(),
                        request.scheduledEndAt(),
                        request.meetingRoomId(),
                        request.attendeeUserIds(),
                        request.externalInvitees() == null
                                ? List.of()
                                : request.externalInvitees().stream()
                                        .map(ExternalInviteeRequest::toCommand)
                                        .toList(),
                        request.reviewerUserId(),
                        request.description());
        return ok(MeetingResponse.from(updateMeetingUseCase.execute(command)));
    }

    /**
     * 참석자 시간 겹침 실시간 검사.
     *
     * <p>회의 생성/수정 폼에서 참석자 추가·시간 변경 시 호출해, 선택한 사용자들이 해당 시간대에 이미 다른 활성 회의에 참석/주최로 잡혀 있으면 겹친 (사용자, 회의)
     * 목록을 돌려준다. 겹침이 없으면 빈 목록을 반환한다. 저장을 막는 최종 방어는 생성/수정 시 별도 가드(409 ATTENDEE_TIME_CONFLICT)가 한다.
     */
    @PostMapping("/attendee-availability")
    @RequireUserOrAdmin
    public ApiResponse<AttendeeAvailabilityResponse> checkAttendeeAvailability(
            @Valid @RequestBody AttendeeAvailabilityRequest request) {
        return ok(
                AttendeeAvailabilityResponse.from(
                        checkAttendeeAvailabilityUseCase.execute(
                                request.userIds(),
                                request.scheduledAt(),
                                request.scheduledEndAt(),
                                request.excludeMeetingId())));
    }

    /** 회의 취소. */
    @PostMapping("/{meetingId}/cancel")
    @RequireUserOrAdmin
    public ApiResponse<MeetingResponse> cancelMeeting(
            @CurrentUser AuthenticatedUser currentUser, @PathVariable UUID meetingId) {
        MeetingResult result = cancelMeetingUseCase.execute(meetingId, currentUser.userId());
        return ok(MeetingResponse.from(result));
    }

    /**
     * 회의 입장용 LiveKit 연결 정보를 발급한다.
     *
     * <p>로그인 사용자는 userId를 기반으로 참여자를 식별하고, 비로그인 요청은 guest/public 경로 정책에 따라 별도 처리된다.
     */
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
                                currentUser != null ? currentUser.organizationId() : null,
                                request.displayName(),
                                request.participantIdentity()));
        return ok(JoinMeetingResponse.from(result));
    }

    /**
     * 회의 종료를 요청한다.
     *
     * <p>현재는 회의 생성자(로그인 사용자) 기준으로만 종료를 허용하고, 회의가 끝나면 FE가 LiveKit DataChannel로 나머지 참여자를 함께 내보낸다.
     */
    @PostMapping("/{meetingId}/end")
    public ApiResponse<EndMeetingResponse> endMeeting(
            @PathVariable UUID meetingId,
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
        if (currentUser != null) {
            MeetingResult meeting =
                    getMeetingUseCase.getById(
                            meetingId, currentUser.userId(), currentUser.isAdmin());
            if (!meeting.hostUserId().equals(currentUser.userId())) {
                throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "회의 주최자만 종료할 수 있습니다.");
            }
        }
        EndMeetingResult result =
                endMeetingUseCase.execute(
                        new EndMeetingCommand(
                                meetingId,
                                Instant.now(),
                                UUID.randomUUID(),
                                "meeting-ended",
                                currentUser != null
                                        ? currentUser.displayName()
                                        : "meeting-client"));
        return ok(EndMeetingResponse.from(result));
    }

    /**
     * LiveKit 채널이 열려 실제 회의가 시작됐음을 기록한다.
     *
     * <p>프론트엔드는 room.connect() 성공 직후 이 API를 best-effort로 호출한다. 이미 진행 중이면 멱등적으로 무시하고, 종료/취소된 회의만 예외로
     * 막는다.
     */
    @PostMapping("/{meetingId}/start")
    public ApiResponse<Void> startMeeting(@PathVariable UUID meetingId) {
        startMeetingUseCase.execute(meetingId);
        return ok();
    }

    /**
     * 회의 관리자 이전.
     *
     * <p>호스트가 잠시 자리를 비우거나 화면 공유 중 역할을 넘겨야 할 때, 현재 참여자 중 한 명을 새 호스트로 지정한다.
     */
    @PostMapping("/{meetingId}/transfer-host")
    public ApiResponse<MeetingResponse> transferHost(
            @PathVariable UUID meetingId,
            @CurrentUser(required = false) AuthenticatedUser currentUser,
            @Valid @RequestBody TransferMeetingHostRequest request) {
        MeetingResult result =
                transferMeetingHostUseCase.execute(
                        new TransferMeetingHostCommand(
                                meetingId,
                                currentUser != null ? currentUser.userId() : null,
                                request.newHostUserId()));
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
