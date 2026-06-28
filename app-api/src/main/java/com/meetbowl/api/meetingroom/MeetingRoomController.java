package com.meetbowl.api.meetingroom;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.RequireUserOrAdmin;
import com.meetbowl.application.meetingroom.GetMeetingRoomStatusUseCase;
import com.meetbowl.application.meetingroom.GetMeetingRoomsQuery;
import com.meetbowl.application.meetingroom.GetMeetingRoomsUseCase;
import com.meetbowl.application.meetingroom.MeetingRoomListItemResult;
import com.meetbowl.application.meetingroom.RoomStatusQuery;
import com.meetbowl.common.response.ApiResponse;
import com.meetbowl.common.response.PageResponse;

/**
 * 회의실 목록·현황 조회 API다(F3). 예약 화면에서 사용자가 자기 회의에 맞는 회의실을 찾는 용도다.
 *
 * <ul>
 *   <li>{@code GET /meeting-rooms} — 사이트/건물/정원 조건으로 거른 회의실 목록(지점 드롭다운 선택값을 siteId로 받아 해당 지점 회의실을
 *       반환).
 *   <li>{@code GET /meeting-rooms/status} — 지정 시간대 기준 회의실별 가용 상태(가능/예약됨/사용중/사용제한).
 * </ul>
 *
 * <p>조회 전용이며 예약 생성은 회의 생성(POST /meetings)이 담당한다. 사용자용이라 {@code /admin} 접두어를 두지 않는다(관리자 회의실 CRUD는
 * {@code /admin/meeting-rooms}).
 */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/meeting-rooms")
@RequireUserOrAdmin
public class MeetingRoomController extends BaseController {

    private final GetMeetingRoomsUseCase getMeetingRoomsUseCase;
    private final GetMeetingRoomStatusUseCase getMeetingRoomStatusUseCase;

    public MeetingRoomController(
            GetMeetingRoomsUseCase getMeetingRoomsUseCase,
            GetMeetingRoomStatusUseCase getMeetingRoomStatusUseCase) {
        this.getMeetingRoomsUseCase = getMeetingRoomsUseCase;
        this.getMeetingRoomStatusUseCase = getMeetingRoomStatusUseCase;
    }

    /** 회의실 목록 조회(사이트/건물/수용인원 필터, page는 1부터). */
    @GetMapping
    public ApiResponse<PageResponse<MeetingRoomListItemResponse>> getMeetingRooms(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestParam(required = false) UUID siteId,
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<MeetingRoomListItemResult> result =
                getMeetingRoomsUseCase.execute(
                        new GetMeetingRoomsQuery(siteId, buildingId, minCapacity, page, size),
                        currentUser.organizationId());
        PageResponse<MeetingRoomListItemResponse> response =
                PageResponse.of(
                        result.items().stream().map(MeetingRoomListItemResponse::from).toList(),
                        result.page(),
                        result.size(),
                        result.totalElements());
        return ok(response);
    }

    /**
     * 회의실 현황 조회(F3). {@code from}~{@code to} 시간대 기준으로 각 회의실의
     * 상태(AVAILABLE/RESERVED/IN_USE/UNAVAILABLE)를 반환한다. 사이트/건물로 필터링한다.
     */
    @GetMapping("/status")
    public ApiResponse<List<RoomStatusResponse>> getMeetingRoomStatuses(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID siteId,
            @RequestParam(required = false) UUID buildingId) {
        List<RoomStatusResponse> responses =
                getMeetingRoomStatusUseCase
                        .execute(
                                new RoomStatusQuery(from, to, siteId, buildingId),
                                currentUser.organizationId())
                        .stream()
                        .map(RoomStatusResponse::from)
                        .toList();
        return ok(responses);
    }
}
