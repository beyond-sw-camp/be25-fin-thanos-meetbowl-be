package com.meetbowl.api.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.application.admin.AdminDashboardSummaryResult;

public record AdminDashboardSummaryResponse(
        List<RecentAuditLogResponse> recentAuditLogs,
        MailRetentionPolicySummaryResponse mailRetentionPolicy,
        MeetingRoomSummaryResponse meetingRoomSummary) {

    public static AdminDashboardSummaryResponse from(AdminDashboardSummaryResult result) {
        return new AdminDashboardSummaryResponse(
                result.recentAuditLogs().stream().map(RecentAuditLogResponse::from).toList(),
                MailRetentionPolicySummaryResponse.from(result.mailRetentionPolicy()),
                MeetingRoomSummaryResponse.from(result.meetingRoomSummary()));
    }

    public record RecentAuditLogResponse(
            UUID auditLogId,
            String actorName,
            String actionType,
            String targetType,
            UUID targetId,
            String result,
            Instant createdAt) {

        static RecentAuditLogResponse from(
                AdminDashboardSummaryResult.RecentAuditLogSummaryResult result) {
            return new RecentAuditLogResponse(
                    result.auditLogId(),
                    result.actorName(),
                    result.actionType(),
                    result.targetType(),
                    result.targetId(),
                    result.result(),
                    result.createdAt());
        }
    }

    public record MailRetentionPolicySummaryResponse(
            int retentionDays, boolean autoDeleteEnabled, Instant updatedAt, UUID updatedBy) {

        static MailRetentionPolicySummaryResponse from(
                com.meetbowl.application.mail.MailRetentionPolicyResult result) {
            return new MailRetentionPolicySummaryResponse(
                    result.retentionDays(),
                    result.autoDeleteEnabled(),
                    result.updatedAt(),
                    result.updatedBy());
        }
    }

    public record MeetingRoomSummaryResponse(
            int todayReservationCount,
            int inUseMeetingRoomCount,
            int availableMeetingRoomCount,
            List<TimeSlotUsageResponse> timeSlotUsage,
            List<TimeSlotUsageResponse> timeSlotOccupancyUsage,
            List<SiteBuildingUsageResponse> siteBuildingUsage) {

        static MeetingRoomSummaryResponse from(
                AdminDashboardSummaryResult.MeetingRoomSummaryResult result) {
            return new MeetingRoomSummaryResponse(
                    result.todayReservationCount(),
                    result.inUseMeetingRoomCount(),
                    result.availableMeetingRoomCount(),
                    result.timeSlotUsage().stream().map(TimeSlotUsageResponse::from).toList(),
                    result.timeSlotOccupancyUsage().stream()
                            .map(TimeSlotUsageResponse::from)
                            .toList(),
                    result.siteBuildingUsage().stream()
                            .map(SiteBuildingUsageResponse::from)
                            .toList());
        }
    }

    public record TimeSlotUsageResponse(Instant slotStartAt, int reservationCount) {

        static TimeSlotUsageResponse from(AdminDashboardSummaryResult.TimeSlotUsageResult result) {
            return new TimeSlotUsageResponse(result.slotStartAt(), result.reservationCount());
        }
    }

    public record SiteBuildingUsageResponse(
            UUID siteId,
            String siteName,
            UUID buildingId,
            String buildingName,
            int totalRooms,
            int usedRooms,
            double usageRate) {

        static SiteBuildingUsageResponse from(
                AdminDashboardSummaryResult.SiteBuildingUsageResult result) {
            return new SiteBuildingUsageResponse(
                    result.siteId(),
                    result.siteName(),
                    result.buildingId(),
                    result.buildingName(),
                    result.totalRooms(),
                    result.usedRooms(),
                    result.usageRate());
        }
    }
}
