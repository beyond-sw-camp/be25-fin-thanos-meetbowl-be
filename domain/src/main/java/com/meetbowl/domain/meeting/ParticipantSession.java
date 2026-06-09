package com.meetbowl.domain.meeting;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 회원 또는 URL 게스트 한 명이 LiveKit 회의방에 접속한 상태를 표현하는 도메인 모델이다.
 *
 * <p>회의와 LiveKit 실행 정보가 1:1로 통합되므로 별도 MeetingSession ID를 사용하지 않고 meetingId를 직접 연결 기준으로 사용한다. 초대
 * URL과 게스트 인증 정보는 입장 전에 검증하며 참가자 접속 이력에는 저장하지 않는다.
 */
public class ParticipantSession {

    /** 영속화된 참가자 접속 세션 식별자이며 신규 생성 시에는 null이다. */
    private final UUID id;

    /** 참가자가 접속한 회의의 식별자다. 참가자 목록과 연결 상태 조회의 기준이 된다. */
    private final UUID meetingId;

    /** 회원과 게스트의 인증 방식 및 회의 내 역할을 구분한다. */
    private final ParticipantType participantType;

    /** 회원 참가자의 사용자 ID다. URL 게스트에게는 반드시 null이어야 한다. */
    private final UUID userId;

    /** 회의 화면에 표시할 이름의 입장 시점 스냅샷이다. */
    private final String displayName;

    /** LiveKit에서 참가자 연결을 고유하게 구분하는 identity다. */
    private final String providerIdentity;

    /** 입장 요청, 연결 완료, 연결 종료 여부를 나타내는 서버 기준 상태다. */
    private final ParticipantSessionStatus status;

    /** LiveKit 회의방 입장이 완료된 시각이다. */
    private final Instant joinedAt;

    /** 연결 상태 이벤트를 마지막으로 확인한 시각이다. */
    private final Instant lastSeenAt;

    private ParticipantSession(
            UUID id,
            UUID meetingId,
            ParticipantType participantType,
            UUID userId,
            String displayName,
            String providerIdentity,
            ParticipantSessionStatus status,
            Instant joinedAt,
            Instant lastSeenAt) {
        this.id = id;
        this.meetingId = meetingId;
        this.participantType = participantType;
        this.userId = userId;
        this.displayName = displayName;
        this.providerIdentity = providerIdentity;
        this.status = status;
        this.joinedAt = joinedAt;
        this.lastSeenAt = lastSeenAt;
    }

    /**
     * 인증된 주최자 또는 일반 회원의 입장 요청 세션을 생성한다.
     *
     * <p>회원 생성 경로에서는 인증 사용자 ID가 필수이며 GUEST 유형을 허용하지 않는다.
     */
    public static ParticipantSession createMember(
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
                meetingId,
                participantType,
                userId,
                displayName,
                providerIdentity,
                ParticipantSessionStatus.JOIN_REQUESTED,
                null,
                null);
    }

    /**
     * 초대 URL 또는 코드의 유효성 검증이 완료된 게스트 접속 세션을 생성한다.
     *
     * <p>게스트 권한 정보는 짧은 TTL 저장소 또는 서명 토큰에서 관리하므로 MariaDB 참가자 행에는 게스트 세션 식별자를 남기지 않는다.
     */
    public static ParticipantSession createGuest(
            UUID meetingId, String displayName, String providerIdentity) {
        return of(
                null,
                meetingId,
                ParticipantType.GUEST,
                null,
                displayName,
                providerIdentity,
                ParticipantSessionStatus.JOIN_REQUESTED,
                null,
                null);
    }

    /**
     * 저장된 참가자 접속 정보를 복원하면서 회원과 게스트의 사용자 ID 규칙을 검증한다.
     *
     * <p>회원은 userId가 필수이고 게스트는 userId가 없어야 한다. 게스트 URL 검증은 이 모델 생성 전에 완료되어야 한다.
     */
    public static ParticipantSession of(
            UUID id,
            UUID meetingId,
            ParticipantType participantType,
            UUID userId,
            String displayName,
            String providerIdentity,
            ParticipantSessionStatus status,
            Instant joinedAt,
            Instant lastSeenAt) {
        require(meetingId, "회의 ID는 필수입니다.");
        require(participantType, "참가자 유형은 필수입니다.");
        require(status, "참가자 세션 상태는 필수입니다.");

        // 표시명과 provider identity는 참가자 화면 표시와 LiveKit 연결 식별에 직접 사용된다.
        if (displayName == null || displayName.isBlank()) {
            throw invalid("참가자 표시 이름은 필수입니다.");
        }
        if (providerIdentity == null || providerIdentity.isBlank()) {
            throw invalid("공급자 참가자 식별자는 필수입니다.");
        }

        // 게스트에게 회원 ID가 남으면 회원 전용 API 권한이 잘못 적용될 수 있다.
        if (participantType == ParticipantType.GUEST) {
            if (userId != null) {
                throw invalid("게스트 참가자는 사용자 ID를 가질 수 없습니다.");
            }
        } else if (userId == null) {
            throw invalid("회원 참가자는 사용자 ID가 필요합니다.");
        }

        return new ParticipantSession(
                id,
                meetingId,
                participantType,
                userId,
                displayName,
                providerIdentity,
                status,
                joinedAt,
                lastSeenAt);
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

    public ParticipantType participantType() {
        return participantType;
    }

    public UUID userId() {
        return userId;
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
}
