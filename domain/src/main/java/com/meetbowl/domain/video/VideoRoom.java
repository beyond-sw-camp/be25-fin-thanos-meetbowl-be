package com.meetbowl.domain.video;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** 회의 하나에 대응되는 화상회의 미디어 세션이다. */
public class VideoRoom {

    private final UUID id;
    private final UUID meetingId;
    private final UUID organizationId;
    private final UUID hostUserId;
    private final VideoProvider provider;
    private final String providerRoomId;
    private final VideoRoomStatus status;
    private final Instant openedAt;
    private final Instant startedAt;
    private final Instant endedAt;
    private final VideoRoomEndReason endReason;

    private VideoRoom(
            UUID id,
            UUID meetingId,
            UUID organizationId,
            UUID hostUserId,
            VideoProvider provider,
            String providerRoomId,
            VideoRoomStatus status,
            Instant openedAt,
            Instant startedAt,
            Instant endedAt,
            VideoRoomEndReason endReason) {
        this.id = id;
        this.meetingId = meetingId;
        this.organizationId = organizationId;
        this.hostUserId = hostUserId;
        this.provider = provider;
        this.providerRoomId = providerRoomId;
        this.status = status;
        this.openedAt = openedAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.endReason = endReason;
    }

    public static VideoRoom create(
            UUID meetingId,
            UUID organizationId,
            UUID hostUserId,
            VideoProvider provider,
            String providerRoomId) {
        return of(
                null,
                meetingId,
                organizationId,
                hostUserId,
                provider,
                providerRoomId,
                VideoRoomStatus.READY,
                null,
                null,
                null,
                null);
    }

    public static VideoRoom of(
            UUID id,
            UUID meetingId,
            UUID organizationId,
            UUID hostUserId,
            VideoProvider provider,
            String providerRoomId,
            VideoRoomStatus status,
            Instant openedAt,
            Instant startedAt,
            Instant endedAt,
            VideoRoomEndReason endReason) {
        require(meetingId, "회의 ID는 필수입니다.");
        require(organizationId, "조직 ID는 필수입니다.");
        require(hostUserId, "주최자 ID는 필수입니다.");
        require(provider, "화상회의 공급자는 필수입니다.");
        require(status, "화상회의방 상태는 필수입니다.");
        if (providerRoomId == null || providerRoomId.isBlank()) {
            throw invalid("공급자 회의방 ID는 필수입니다.");
        }
        if (startedAt != null && endedAt != null && endedAt.isBefore(startedAt)) {
            throw invalid("화상회의 종료 시각은 시작 시각보다 빠를 수 없습니다.");
        }
        if (status == VideoRoomStatus.ENDED && (endedAt == null || endReason == null)) {
            throw invalid("종료된 화상회의방에는 종료 시각과 종료 사유가 필요합니다.");
        }
        return new VideoRoom(
                id,
                meetingId,
                organizationId,
                hostUserId,
                provider,
                providerRoomId,
                status,
                openedAt,
                startedAt,
                endedAt,
                endReason);
    }

    private static void require(Object value, String message) {
        if (value == null) {
            throw invalid(message);
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
    }

    public UUID id() {
        return id;
    }

    public UUID meetingId() {
        return meetingId;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID hostUserId() {
        return hostUserId;
    }

    public VideoProvider provider() {
        return provider;
    }

    public String providerRoomId() {
        return providerRoomId;
    }

    public VideoRoomStatus status() {
        return status;
    }

    public Instant openedAt() {
        return openedAt;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant endedAt() {
        return endedAt;
    }

    public VideoRoomEndReason endReason() {
        return endReason;
    }
}
