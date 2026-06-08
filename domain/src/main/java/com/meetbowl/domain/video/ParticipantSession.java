package com.meetbowl.domain.video;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** 회원 또는 게스트가 화상회의방에 접속한 이력이다. */
public class ParticipantSession {

    private final UUID id;
    private final UUID videoRoomId;
    private final UUID meetingId;
    private final ParticipantType participantType;
    private final UUID userId;
    private final UUID guestSessionId;
    private final String displayName;
    private final String providerIdentity;
    private final ParticipantSessionStatus status;
    private final Instant joinedAt;
    private final Instant lastSeenAt;
    private final Instant leftAt;
    private final ParticipantLeaveReason leaveReason;

    private ParticipantSession(
            UUID id,
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
        this.id = id;
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

    public static ParticipantSession createMember(
            UUID videoRoomId,
            UUID meetingId,
            ParticipantType participantType,
            UUID userId,
            String displayName,
            String providerIdentity) {
        if (participantType == ParticipantType.GUEST) {
            throw invalid("회원 참가자 유형은 GUEST일 수 없습니다.");
        }
        return of(
                null,
                videoRoomId,
                meetingId,
                participantType,
                userId,
                null,
                displayName,
                providerIdentity,
                ParticipantSessionStatus.JOIN_REQUESTED,
                null,
                null,
                null,
                null);
    }

    public static ParticipantSession createGuest(
            UUID videoRoomId,
            UUID meetingId,
            UUID guestSessionId,
            String displayName,
            String providerIdentity) {
        return of(
                null,
                videoRoomId,
                meetingId,
                ParticipantType.GUEST,
                null,
                guestSessionId,
                displayName,
                providerIdentity,
                ParticipantSessionStatus.JOIN_REQUESTED,
                null,
                null,
                null,
                null);
    }

    public static ParticipantSession of(
            UUID id,
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
        require(videoRoomId, "화상회의방 ID는 필수입니다.");
        require(meetingId, "회의 ID는 필수입니다.");
        require(participantType, "참가자 유형은 필수입니다.");
        require(status, "참가자 세션 상태는 필수입니다.");
        if (displayName == null || displayName.isBlank()) {
            throw invalid("참가자 표시 이름은 필수입니다.");
        }
        if (providerIdentity == null || providerIdentity.isBlank()) {
            throw invalid("공급자 참가자 식별자는 필수입니다.");
        }
        if (participantType == ParticipantType.GUEST) {
            if (guestSessionId == null || userId != null) {
                throw invalid("게스트 참가자는 게스트 세션 ID만 가져야 합니다.");
            }
        } else if (userId == null || guestSessionId != null) {
            throw invalid("회원 참가자는 사용자 ID만 가져야 합니다.");
        }
        if (joinedAt != null && leftAt != null && leftAt.isBefore(joinedAt)) {
            throw invalid("퇴장 시각은 입장 시각보다 빠를 수 없습니다.");
        }
        if (isLeftStatus(status) && (leftAt == null || leaveReason == null)) {
            throw invalid("종료된 참가자 세션에는 퇴장 시각과 사유가 필요합니다.");
        }
        return new ParticipantSession(
                id,
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

    private static boolean isLeftStatus(ParticipantSessionStatus status) {
        return status == ParticipantSessionStatus.LEFT
                || status == ParticipantSessionStatus.DISCONNECTED
                || status == ParticipantSessionStatus.KICKED;
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

    public UUID videoRoomId() {
        return videoRoomId;
    }

    public UUID meetingId() {
        return meetingId;
    }

    public ParticipantType participantType() {
        return participantType;
    }

    public UUID userId() {
        return userId;
    }

    public UUID guestSessionId() {
        return guestSessionId;
    }

    public String displayName() {
        return displayName;
    }

    public String providerIdentity() {
        return providerIdentity;
    }

    public ParticipantSessionStatus status() {
        return status;
    }

    public Instant joinedAt() {
        return joinedAt;
    }

    public Instant lastSeenAt() {
        return lastSeenAt;
    }

    public Instant leftAt() {
        return leftAt;
    }

    public ParticipantLeaveReason leaveReason() {
        return leaveReason;
    }
}
