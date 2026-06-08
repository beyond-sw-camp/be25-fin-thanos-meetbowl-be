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

import com.meetbowl.domain.video.ParticipantLeaveReason;
import com.meetbowl.domain.video.ParticipantSession;
import com.meetbowl.domain.video.ParticipantSessionStatus;
import com.meetbowl.domain.video.ParticipantType;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** 회원 또는 게스트의 화상회의 접속 이력을 저장하는 JPA Entity다. */
@Entity
@Table(
        name = "participant_sessions",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_participant_sessions_provider_identity",
                    columnNames = {"video_room_id", "provider_identity"})
        },
        indexes = {
            @Index(
                    name = "idx_participant_sessions_room_status",
                    columnList = "video_room_id, status"),
            @Index(name = "idx_participant_sessions_meeting", columnList = "meeting_id"),
            @Index(name = "idx_participant_sessions_user", columnList = "user_id"),
            @Index(name = "idx_participant_sessions_guest", columnList = "guest_session_id")
        })
public class ParticipantSessionEntity extends BaseEntity {

    @Column(
            name = "video_room_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID videoRoomId;

    @Column(
            name = "meeting_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID meetingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, updatable = false, length = 30)
    private ParticipantType participantType;

    @Column(name = "user_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "guest_session_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID guestSessionId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "provider_identity", nullable = false, updatable = false, length = 255)
    private String providerIdentity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ParticipantSessionStatus status;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_reason", length = 30)
    private ParticipantLeaveReason leaveReason;

    protected ParticipantSessionEntity() {}

    private ParticipantSessionEntity(
            UUID videoRoomId,
            UUID meetingId,
            ParticipantType participantType,
            UUID userId,
            UUID guestSessionId,
            String displayName,
            String providerIdentity,
            ParticipantSessionStatus status,
            Instant joinedAt,
            Instant lastSeenAt,
            Instant leftAt,
            ParticipantLeaveReason leaveReason) {
        this.videoRoomId = videoRoomId;
        this.meetingId = meetingId;
        this.participantType = participantType;
        this.userId = userId;
        this.guestSessionId = guestSessionId;
        this.displayName = displayName;
        this.providerIdentity = providerIdentity;
        this.status = status;
        this.joinedAt = joinedAt;
        this.lastSeenAt = lastSeenAt;
        this.leftAt = leftAt;
        this.leaveReason = leaveReason;
    }

    static ParticipantSessionEntity from(ParticipantSession participantSession) {
        ParticipantSessionEntity entity =
                new ParticipantSessionEntity(
                        participantSession.videoRoomId(),
                        participantSession.meetingId(),
                        participantSession.participantType(),
                        participantSession.userId(),
                        participantSession.guestSessionId(),
                        participantSession.displayName(),
                        participantSession.providerIdentity(),
                        participantSession.status(),
                        participantSession.joinedAt(),
                        participantSession.lastSeenAt(),
                        participantSession.leftAt(),
                        participantSession.leaveReason());
        entity.setId(participantSession.id());
        return entity;
    }

    ParticipantSession toDomain() {
        return ParticipantSession.of(
                getId(),
                videoRoomId,
                meetingId,
                participantType,
                userId,
                guestSessionId,
                displayName,
                providerIdentity,
                status,
                joinedAt,
                lastSeenAt,
                leftAt,
                leaveReason);
    }
}
