package com.meetbowl.infrastructure.persistence.video;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.video.VideoProvider;
import com.meetbowl.domain.video.VideoRoom;
import com.meetbowl.domain.video.VideoRoomEndReason;
import com.meetbowl.domain.video.VideoRoomStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** 화상회의 공급자의 회의방과 Meetbowl 회의를 연결하는 JPA Entity다. */
@Entity
@Table(
        name = "video_rooms",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_video_rooms_meeting", columnNames = "meeting_id"),
            @UniqueConstraint(
                    name = "uk_video_rooms_provider_room",
                    columnNames = {"provider", "provider_room_id"})
        },
        indexes = {
            @Index(
                    name = "idx_video_rooms_organization_status",
                    columnList = "organization_id, status")
        })
public class VideoRoomEntity extends BaseEntity {

    @Column(
            name = "meeting_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID meetingId;

    @Column(
            name = "organization_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID organizationId;

    @Column(
            name = "host_user_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID hostUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VideoProvider provider;

    @Column(name = "provider_room_id", nullable = false, length = 255)
    private String providerRoomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VideoRoomStatus status;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", length = 30)
    private VideoRoomEndReason endReason;

    protected VideoRoomEntity() {}

    private VideoRoomEntity(
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

    static VideoRoomEntity from(VideoRoom videoRoom) {
        VideoRoomEntity entity =
                new VideoRoomEntity(
                        videoRoom.meetingId(),
                        videoRoom.organizationId(),
                        videoRoom.hostUserId(),
                        videoRoom.provider(),
                        videoRoom.providerRoomId(),
                        videoRoom.status(),
                        videoRoom.openedAt(),
                        videoRoom.startedAt(),
                        videoRoom.endedAt(),
                        videoRoom.endReason());
        entity.setId(videoRoom.id());
        return entity;
    }

    VideoRoom toDomain() {
        return VideoRoom.of(
                getId(),
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
}
