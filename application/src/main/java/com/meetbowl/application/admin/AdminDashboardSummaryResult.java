package com.meetbowl.application.admin;

import java.util.List;
import java.util.UUID;

import com.meetbowl.application.mail.MailRetentionPolicyResult;

public record AdminDashboardSummaryResult(
        List<RecentAuditLogSummaryResult> recentAuditLogs,
        MailRetentionPolicyResult mailRetentionPolicy,
        MeetingRoomSummaryResult meetingRoomSummary) {

    public record RecentAuditLogSummaryResult(
            UUID auditLogId,
            String actorName,
            String actionType,
            String targetType,
            UUID targetId,
            String result,
            java.time.Instant createdAt) {}

    public record MeetingRoomSummaryResult(
            int todayReservationCount,
            int inUseMeetingRoomCount,
            int availableMeetingRoomCount,
            List<TimeSlotUsageResult> timeSlotUsage,
            List<TimeSlotUsageResult> timeSlotOccupancyUsage,
            List<WeekdayReservationUsageResult> weekdayReservationUsage,
            List<SiteBuildingUsageResult> siteBuildingUsage) {}

    public record TimeSlotUsageResult(java.time.Instant slotStartAt, int reservationCount) {}

    public record WeekdayReservationUsageResult(
            int dayOfWeek, String weekdayLabel, int reservationCount) {}

    public record SiteBuildingUsageResult(
            UUID siteId,
            String siteName,
            UUID buildingId,
            String buildingName,
            int totalRooms,
            int usedRooms,
            double usageRate) {}
}
