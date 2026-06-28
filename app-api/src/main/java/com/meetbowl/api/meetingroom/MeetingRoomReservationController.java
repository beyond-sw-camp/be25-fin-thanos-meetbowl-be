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
import com.meetbowl.application.meeting.MeetingListFilter;
import com.meetbowl.application.meetingroom.GetRoomReservationsUseCase;
import com.meetbowl.common.response.ApiResponse;

/**
 * 회의실 예약 현황 API다(F4). 예약은 회의실을 점유한 회의이며, 회의실 도메인 관점으로 노출한다.
 *
 * <ul>
 *   <li>{@code GET /meeting-rooms/reservations/me} — 내가 예약(host)했거나 참석할(invited) 회의실 회의 목록(화면 ①②)
 *   <li>{@code GET /meeting-rooms/reservations} — 시간대별 회의실 예약 타임라인(화면 ③, 모든 사용자 예약)
 * </ul>
 */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/meeting-rooms/reservations")
@RequireUserOrAdmin
public class MeetingRoomReservationController extends BaseController {

    private final GetRoomReservationsUseCase getRoomReservationsUseCase;

    public MeetingRoomReservationController(GetRoomReservationsUseCase getRoomReservationsUseCase) {
        this.getRoomReservationsUseCase = getRoomReservationsUseCase;
    }

    /** 내 회의실 예약 목록. {@code role}=host(내가 예약한)/invited(내가 참석할)/all(둘 다). */
    @GetMapping("/me")
    public ApiResponse<List<ReservationItemResponse>> getMyReservations(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "all") String role) {
        List<ReservationItemResponse> responses =
                getRoomReservationsUseCase
                        .getMyReservations(
                                currentUser.userId(),
                                toFilter(role),
                                currentUser.organizationId())
                        .stream()
                        .map(ReservationItemResponse::from)
                        .toList();
        return ok(responses);
    }

    /** 회의실 예약 타임라인. {@code from}~{@code to} 시간대의 회의실별 예약 블록을 반환한다. */
    @GetMapping
    public ApiResponse<List<RoomReservationsResponse>> getReservationBoard(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID siteId,
            @RequestParam(required = false) UUID buildingId) {
        List<RoomReservationsResponse> responses =
                getRoomReservationsUseCase
                        .getReservationBoard(
                                from, to, siteId, buildingId, currentUser.organizationId())
                        .stream()
                        .map(RoomReservationsResponse::from)
                        .toList();
        return ok(responses);
    }

    private MeetingListFilter toFilter(String role) {
        return switch (role == null ? "all" : role.toLowerCase()) {
            case "host" -> MeetingListFilter.HOST;
            case "invited" -> MeetingListFilter.INVITED;
            default -> MeetingListFilter.ALL;
        };
    }
}
