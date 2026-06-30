package com.meetbowl.application.admin;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.application.mail.MailRetentionPolicyUseCase;
import com.meetbowl.application.meetingroom.GetMeetingRoomStatusUseCase;
import com.meetbowl.application.meetingroom.RoomAvailabilityStatus;
import com.meetbowl.application.meetingroom.RoomStatusQuery;
import com.meetbowl.application.meetingroom.RoomStatusResult;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;

@Service
public class AdminDashboardSummaryUseCase {

    private static final int RECENT_AUDIT_LOG_LIMIT = 5;
    private static final int HOURS_PER_DAY = 24;

    private final AdminAuditLogQueryUseCase adminAuditLogQueryUseCase;
    private final MailRetentionPolicyUseCase mailRetentionPolicyUseCase;
    private final GetMeetingRoomStatusUseCase getMeetingRoomStatusUseCase;
    private final MeetingRepositoryPort meetingRepositoryPort;
    private final Clock clock;

    public AdminDashboardSummaryUseCase(
            AdminAuditLogQueryUseCase adminAuditLogQueryUseCase,
            MailRetentionPolicyUseCase mailRetentionPolicyUseCase,
            GetMeetingRoomStatusUseCase getMeetingRoomStatusUseCase,
            MeetingRepositoryPort meetingRepositoryPort,
            Clock clock) {
        this.adminAuditLogQueryUseCase = adminAuditLogQueryUseCase;
        this.mailRetentionPolicyUseCase = mailRetentionPolicyUseCase;
        this.getMeetingRoomStatusUseCase = getMeetingRoomStatusUseCase;
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminDashboardSummaryResult get() {
        Instant now = Instant.now(clock);
        // 현재 회의실 상태는 "지금 이 순간" 기준으로만 판단하면 되므로 아주 짧은 조회 구간만 사용한다.
        Instant currentWindowEnd = now.plusSeconds(1);
        Instant todayStart = todayStart(now);
        Instant tomorrowStart = todayStart.plusSeconds(24L * 60L * 60L);

        List<RoomStatusResult> roomStatuses =
                getMeetingRoomStatusUseCase.execute(
                        new RoomStatusQuery(now, currentWindowEnd, null, null));
        List<Meeting> todayMeetings =
                meetingRepositoryPort.findNonCancelledRoomMeetingsOverlapping(
                        todayStart, tomorrowStart);

        return new AdminDashboardSummaryResult(
                recentAuditLogs(),
                mailRetentionPolicyUseCase.get(),
                new AdminDashboardSummaryResult.MeetingRoomSummaryResult(
                        todayReservationCount(todayMeetings, todayStart, tomorrowStart),
                        inUseMeetingRoomCount(roomStatuses),
                        availableMeetingRoomCount(roomStatuses),
                        reservationStartUsage(todayMeetings, todayStart),
                        timeSlotOccupancyUsage(todayMeetings, todayStart),
                        siteBuildingUsage(roomStatuses)));
    }

    private List<AdminDashboardSummaryResult.RecentAuditLogSummaryResult> recentAuditLogs() {
        return adminAuditLogQueryUseCase
                .search(
                        new AdminAuditLogSearchCommand(
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                1,
                                RECENT_AUDIT_LOG_LIMIT))
                .items()
                .stream()
                .limit(RECENT_AUDIT_LOG_LIMIT)
                .map(
                        item ->
                                new AdminDashboardSummaryResult.RecentAuditLogSummaryResult(
                                        item.auditLogId(),
                                        item.actorName(),
                                        item.actionType(),
                                        item.targetType(),
                                        item.targetId(),
                                        item.result(),
                                        item.createdAt()))
                .toList();
    }

    private int todayReservationCount(
            List<Meeting> meetings, Instant todayStart, Instant tomorrowStart) {
        // "오늘 예약 수"는 오늘 시작한 예약 건수 기준이므로, 자정을 걸쳐 이어지는 어제 예약은 제외한다.
        return (int)
                meetings.stream()
                        .filter(meeting -> !meeting.scheduledAt().isBefore(todayStart))
                        .filter(meeting -> meeting.scheduledAt().isBefore(tomorrowStart))
                        .count();
    }

    private int inUseMeetingRoomCount(List<RoomStatusResult> roomStatuses) {
        return (int)
                roomStatuses.stream()
                        .filter(room -> room.status() == RoomAvailabilityStatus.IN_USE)
                        .count();
    }

    private int availableMeetingRoomCount(List<RoomStatusResult> roomStatuses) {
        return (int)
                roomStatuses.stream()
                        .filter(room -> room.status() == RoomAvailabilityStatus.AVAILABLE)
                        .count();
    }

    private List<AdminDashboardSummaryResult.TimeSlotUsageResult> reservationStartUsage(
            List<Meeting> meetings, Instant todayStart) {
        // 예약 시작 빈도 그래프는 "각 슬롯에 시작한 예약 수"를 그대로 보여줘야 한다.
        return IntStream.range(0, HOURS_PER_DAY)
                .mapToObj(
                        hour ->
                                reservationStartSlotUsage(
                                        meetings, todayStart.plusSeconds(hour * 3600L)))
                .toList();
    }

    private List<AdminDashboardSummaryResult.TimeSlotUsageResult> timeSlotOccupancyUsage(
            List<Meeting> meetings, Instant slotStart) {
        // 상세 점유 그래프는 "슬롯과 겹치는 회의실 수"를 보여줘야 하므로 별도 집계를 사용한다.
        return IntStream.range(0, HOURS_PER_DAY)
                .mapToObj(
                        hour ->
                                occupancySlotUsage(
                                        meetings, slotStart.plusSeconds(hour * 3600L)))
                .toList();
    }

    private AdminDashboardSummaryResult.TimeSlotUsageResult reservationStartSlotUsage(
            List<Meeting> meetings, Instant slotStart) {
        Instant slotEnd = slotStart.plusSeconds(3600);
        int reservationCount =
                (int)
                        meetings.stream()
                                .filter(meeting -> !meeting.scheduledAt().isBefore(slotStart))
                                .filter(meeting -> meeting.scheduledAt().isBefore(slotEnd))
                                .count();
        return new AdminDashboardSummaryResult.TimeSlotUsageResult(slotStart, reservationCount);
    }

    private AdminDashboardSummaryResult.TimeSlotUsageResult occupancySlotUsage(
            List<Meeting> meetings, Instant slotStart) {
        Instant slotEnd = slotStart.plusSeconds(3600);
        // 한 슬롯 안에 조금이라도 걸치면 해당 시간대 점유로 본다.
        int reservationCount =
                (int)
                        meetings.stream()
                                .filter(meeting -> overlaps(meeting, slotStart, slotEnd))
                                .count();
        return new AdminDashboardSummaryResult.TimeSlotUsageResult(slotStart, reservationCount);
    }

    private List<AdminDashboardSummaryResult.SiteBuildingUsageResult> siteBuildingUsage(
            List<RoomStatusResult> roomStatuses) {
        // 사용률은 현재 시점 상태 집계이므로 site/building 기준으로 먼저 묶은 뒤 계산한다.
        Map<SiteBuildingKey, List<RoomStatusResult>> grouped =
                roomStatuses.stream()
                        .collect(
                                Collectors.groupingBy(
                                        room ->
                                                new SiteBuildingKey(
                                                        room.siteId(),
                                                        room.siteName(),
                                                        room.buildingId(),
                                                        room.buildingName())));

        return grouped.entrySet().stream()
                .map(entry -> toSiteBuildingUsage(entry.getKey(), entry.getValue()))
                .sorted(
                        java.util.Comparator.comparing(
                                        AdminDashboardSummaryResult.SiteBuildingUsageResult
                                                ::siteName,
                                        java.util.Comparator.nullsLast(String::compareTo))
                                .thenComparing(
                                        AdminDashboardSummaryResult.SiteBuildingUsageResult
                                                ::buildingName,
                                        java.util.Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private AdminDashboardSummaryResult.SiteBuildingUsageResult toSiteBuildingUsage(
            SiteBuildingKey key, List<RoomStatusResult> rooms) {
        int totalRooms = rooms.size();
        int usedRooms =
                (int)
                        rooms.stream()
                                .filter(room -> room.status() == RoomAvailabilityStatus.IN_USE)
                                .count();
        double usageRate = totalRooms == 0 ? 0.0 : (double) usedRooms / totalRooms;
        return new AdminDashboardSummaryResult.SiteBuildingUsageResult(
                key.siteId(),
                key.siteName(),
                key.buildingId(),
                key.buildingName(),
                totalRooms,
                usedRooms,
                usageRate);
    }

    private boolean overlaps(Meeting meeting, Instant from, Instant to) {
        return meeting.scheduledAt().isBefore(to) && meeting.scheduledEndAt().isAfter(from);
    }

    private Instant todayStart(Instant now) {
        // 관리자 대시보드는 화면 기준 시각인 KST 자정 경계로 하루를 묶는다.
        ZoneId kst = ZoneId.of("Asia/Seoul");
        LocalDate today = now.atZone(kst).toLocalDate();
        return today.atStartOfDay(kst).toInstant();
    }

    private record SiteBuildingKey(
            UUID siteId, String siteName, UUID buildingId, String buildingName) {}
}
