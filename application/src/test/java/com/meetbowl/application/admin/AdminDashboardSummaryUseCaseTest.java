package com.meetbowl.application.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetbowl.application.mail.MailRetentionPolicyResult;
import com.meetbowl.application.mail.MailRetentionPolicyUseCase;
import com.meetbowl.application.meetingroom.GetMeetingRoomStatusUseCase;
import com.meetbowl.application.meetingroom.RoomAvailabilityStatus;
import com.meetbowl.application.meetingroom.RoomStatusQuery;
import com.meetbowl.application.meetingroom.RoomStatusResult;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.meeting.MeetingStatus;

@ExtendWith(MockitoExtension.class)
class AdminDashboardSummaryUseCaseTest {

    private static final UUID AUDIT_LOG_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000402");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000403");
    private static final UUID SITE_ID = UUID.fromString("00000000-0000-0000-0000-000000000404");
    private static final UUID BUILDING_A_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000405");
    private static final UUID BUILDING_B_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000406");
    private static final UUID ROOM_A_ID = UUID.fromString("00000000-0000-0000-0000-000000000407");
    private static final UUID ROOM_B_ID = UUID.fromString("00000000-0000-0000-0000-000000000408");
    private static final UUID ROOM_C_ID = UUID.fromString("00000000-0000-0000-0000-000000000409");
    private static final UUID ROOM_D_ID = UUID.fromString("00000000-0000-0000-0000-000000000410");

    @Mock private AdminAuditLogQueryUseCase adminAuditLogQueryUseCase;
    @Mock private MailRetentionPolicyUseCase mailRetentionPolicyUseCase;
    @Mock private GetMeetingRoomStatusUseCase getMeetingRoomStatusUseCase;
    @Mock private MeetingRepositoryPort meetingRepositoryPort;

    @Test
    void getBuildsDashboardSummaryFromExistingQueries() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-13T10:15:30Z"), ZoneOffset.UTC);
        AdminDashboardSummaryUseCase useCase =
                new AdminDashboardSummaryUseCase(
                        adminAuditLogQueryUseCase,
                        mailRetentionPolicyUseCase,
                        getMeetingRoomStatusUseCase,
                        meetingRepositoryPort,
                        fixedClock);

        given(adminAuditLogQueryUseCase.search(any()))
                .willReturn(
                        new AdminAuditLogPageResult(
                                List.of(
                                        new AdminAuditLogResult(
                                                AUDIT_LOG_ID,
                                                ADMIN_ID,
                                                "Admin One",
                                                "USER_UPDATE",
                                                "USER",
                                                TARGET_ID,
                                                "user01",
                                                "User One",
                                                "SUCCESS",
                                                null,
                                                null,
                                                null,
                                                Instant.parse("2026-06-13T09:00:00Z"))),
                                1,
                                5,
                                1,
                                1));
        given(mailRetentionPolicyUseCase.get())
                .willReturn(
                        new MailRetentionPolicyResult(
                                365, false, Instant.parse("2026-06-12T00:00:00Z"), ADMIN_ID));
        given(getMeetingRoomStatusUseCase.execute(any()))
                .willReturn(
                        List.of(
                                roomStatus(
                                        ROOM_A_ID,
                                        "A-1",
                                        BUILDING_A_ID,
                                        "Building A",
                                        RoomAvailabilityStatus.IN_USE),
                                roomStatus(
                                        ROOM_B_ID,
                                        "A-2",
                                        BUILDING_A_ID,
                                        "Building A",
                                        RoomAvailabilityStatus.AVAILABLE),
                                roomStatus(
                                        ROOM_C_ID,
                                        "B-1",
                                        BUILDING_B_ID,
                                        "Building B",
                                        RoomAvailabilityStatus.IN_USE),
                                roomStatus(
                                        ROOM_D_ID,
                                        "B-2",
                                        BUILDING_B_ID,
                                        "Building B",
                                        RoomAvailabilityStatus.RESERVED)));
        given(
                        meetingRepositoryPort.findNonCancelledRoomMeetingsOverlapping(
                                Instant.parse("2026-06-13T00:00:00Z"),
                                Instant.parse("2026-06-14T00:00:00Z")))
                .willReturn(
                        List.of(
                                meeting(
                                        UUID.randomUUID(),
                                        ROOM_A_ID,
                                        Instant.parse("2026-06-13T01:15:00Z"),
                                        Instant.parse("2026-06-13T02:15:00Z"),
                                        MeetingStatus.SCHEDULED),
                                meeting(
                                        UUID.randomUUID(),
                                        ROOM_B_ID,
                                        Instant.parse("2026-06-13T01:45:00Z"),
                                        Instant.parse("2026-06-13T03:00:00Z"),
                                        MeetingStatus.ENDED),
                                meeting(
                                        UUID.randomUUID(),
                                        ROOM_C_ID,
                                        Instant.parse("2026-06-12T23:30:00Z"),
                                        Instant.parse("2026-06-13T00:30:00Z"),
                                        MeetingStatus.ENDED)));

        AdminDashboardSummaryResult result = useCase.get();

        assertEquals(1, result.recentAuditLogs().size());
        assertEquals(AUDIT_LOG_ID, result.recentAuditLogs().get(0).auditLogId());
        assertEquals(365, result.mailRetentionPolicy().retentionDays());
        assertEquals(2, result.meetingRoomSummary().todayReservationCount());
        assertEquals(2, result.meetingRoomSummary().inUseMeetingRoomCount());
        assertEquals(1, result.meetingRoomSummary().availableMeetingRoomCount());
        assertEquals(
                1,
                result.meetingRoomSummary().timeSlotUsage().stream()
                        .filter(
                                slot ->
                                        slot.slotStartAt()
                                                .equals(Instant.parse("2026-06-13T00:00:00Z")))
                        .findFirst()
                        .orElseThrow()
                        .reservationCount());
        assertEquals(
                2,
                result.meetingRoomSummary().timeSlotUsage().stream()
                        .filter(
                                slot ->
                                        slot.slotStartAt()
                                                .equals(Instant.parse("2026-06-13T01:00:00Z")))
                        .findFirst()
                        .orElseThrow()
                        .reservationCount());
        assertEquals(2, result.meetingRoomSummary().siteBuildingUsage().size());
        assertEquals(
                0.5,
                result.meetingRoomSummary().siteBuildingUsage().stream()
                        .filter(item -> item.buildingId().equals(BUILDING_A_ID))
                        .findFirst()
                        .orElseThrow()
                        .usageRate());

        ArgumentCaptor<AdminAuditLogSearchCommand> auditCaptor =
                ArgumentCaptor.forClass(AdminAuditLogSearchCommand.class);
        verify(adminAuditLogQueryUseCase).search(auditCaptor.capture());
        assertEquals(1, auditCaptor.getValue().page());
        assertEquals(5, auditCaptor.getValue().size());

        ArgumentCaptor<RoomStatusQuery> roomStatusCaptor =
                ArgumentCaptor.forClass(RoomStatusQuery.class);
        verify(getMeetingRoomStatusUseCase).execute(roomStatusCaptor.capture());
        assertEquals(Instant.parse("2026-06-13T10:15:30Z"), roomStatusCaptor.getValue().from());
    }

    private RoomStatusResult roomStatus(
            UUID roomId,
            String roomName,
            UUID buildingId,
            String buildingName,
            RoomAvailabilityStatus status) {
        return new RoomStatusResult(
                roomId, roomName, SITE_ID, "HQ", buildingId, buildingName, 8, status, null);
    }

    private Meeting meeting(
            UUID meetingId,
            UUID roomId,
            Instant scheduledAt,
            Instant scheduledEndAt,
            MeetingStatus status) {
        return Meeting.of(
                meetingId,
                "Dashboard Meeting",
                scheduledAt,
                scheduledEndAt,
                UUID.randomUUID(),
                roomId,
                null,
                null,
                status,
                null,
                status == MeetingStatus.ENDED ? scheduledEndAt : null,
                null);
    }
}
