package com.meetbowl.domain.meeting;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 회원 또는 게스트 한 명이 외부 미디어 회의방에 접속한 상태를 표현하는 도메인 모델이다.
 *
 * <p>장치 선택과 마이크/카메라 토글은 브라우저 및 LiveKit 런타임 상태이므로 포함하지 않는다. DB에는 입장 주체, 공급자 identity, 접속 상태처럼 서버가
 * 권한과 참가자 목록을 판단하는 데 필요한 값만 유지한다.
 */
public class ParticipantSession {

    /** 영속화된 참가자 접속 세션 식별자이며 신규 생성 시에는 null이다. */
    private final UUID id;

    /** 참가자가 실제로 접속하는 MeetingSession의 식별자다. */
    private final UUID meetingSessionId;

    /** 상위 회의 기준 조회를 위해 함께 보관하는 회의 도메인 식별자다. */
    private final UUID meetingId;

    /** 회원과 게스트의 식별자 검증 방식 및 회의 내 역할을 결정한다. */
    private final ParticipantType participantType;

    /** 회원 참가자의 사용자 ID다. 게스트 참가자에게는 반드시 null이어야 한다. */
    private final UUID userId;

    /** 게스트 권한 검증 결과로 전달받은 식별자다. 별도 Guest Entity를 의미하지 않는다. */
    private final UUID guestSessionId;

    /** 회의 화면에 표시할 이름의 입장 시점 스냅샷이다. */
    private final String displayName;

    /** LiveKit에서 참가자 연결을 고유하게 구분하는 identity다. */
    private final String providerIdentity;

    /** 입장 요청, 연결 완료, 연결 종료 여부를 나타내는 서버 기준 상태다. */
    private final ParticipantSessionStatus status;

    /** 외부 미디어 세션 연결이 완료된 시각이다. */
    private final Instant joinedAt;

    /** 연결 상태 이벤트를 마지막으로 확인한 시각으로, 비정상 연결 판단에 사용할 수 있다. */
    private final Instant lastSeenAt;

    private ParticipantSession(
            UUID id,
            UUID meetingSessionId,
            UUID meetingId,
            ParticipantType participantType,
            UUID userId,
            UUID guestSessionId,
            String displayName,
            String providerIdentity,
            ParticipantSessionStatus status,
            Instant joinedAt,
            Instant lastSeenAt) {
        this.id = id;
        this.meetingSessionId = meetingSessionId;
        this.meetingId = meetingId;
        this.participantType = participantType;
        this.userId = userId;
        this.guestSessionId = guestSessionId;
        this.displayName = displayName;
        this.providerIdentity = providerIdentity;
        this.status = status;
        this.joinedAt = joinedAt;
        this.lastSeenAt = lastSeenAt;
    }

    /**
     * 인증된 회원의 입장 요청 세션을 생성한다.
     *
     * <p>회원 생성 경로에서 GUEST 유형을 허용하면 userId와 guestSessionId 규칙이 깨지므로 진입 시점에 차단한다.
     */
    public static ParticipantSession createMember(
            UUID meetingSessionId,
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
                meetingSessionId,
                meetingId,
                participantType,
                userId,
                null,
                displayName,
                providerIdentity,
                ParticipantSessionStatus.JOIN_REQUESTED,
                null,
                null);
    }

    /**
     * 별도 초대/권한 기능에서 입장 가능 여부가 확인된 게스트 세션을 생성한다.
     *
     * <p>게스트 자체를 이 도메인에서 영속화하지 않고, 권한 검증 결과 식별자만 접속 이력에 스냅샷으로 보관한다.
     */
    public static ParticipantSession createGuest(
            UUID meetingSessionId,
            UUID meetingId,
            UUID guestSessionId,
            String displayName,
            String providerIdentity) {
        return of(
                null,
                meetingSessionId,
                meetingId,
                ParticipantType.GUEST,
                null,
                guestSessionId,
                displayName,
                providerIdentity,
                ParticipantSessionStatus.JOIN_REQUESTED,
                null,
                null);
    }

    /**
     * 저장된 참가자 접속 정보를 복원하면서 참가자 유형별 식별자 불변조건을 검증한다.
     *
     * <p>회원은 userId만, 게스트는 guestSessionId만 가져야 한다. 두 식별자가 함께 존재하면 권한 주체를 명확히 판단할 수 없기 때문에 복원을 거부한다.
     */
    public static ParticipantSession of(
            UUID id,
            UUID meetingSessionId,
            UUID meetingId,
            ParticipantType participantType,
            UUID userId,
            UUID guestSessionId,
            String displayName,
            String providerIdentity,
            ParticipantSessionStatus status,
            Instant joinedAt,
            Instant lastSeenAt) {
        // 두 회의 식별자는 실시간 세션 조회와 상위 회의 기준 조회에 각각 사용되므로 모두 필요하다.
        require(meetingSessionId, "화상회의방 ID는 필수입니다.");
        require(meetingId, "회의 ID는 필수입니다.");
        require(participantType, "참가자 유형은 필수입니다.");
        require(status, "참가자 세션 상태는 필수입니다.");
        // 표시명과 provider identity는 참가자 화면 표시 및 외부 연결 식별에 직접 사용된다.
        if (displayName == null || displayName.isBlank()) {
            throw invalid("참가자 표시 이름은 필수입니다.");
        }
        if (providerIdentity == null || providerIdentity.isBlank()) {
            throw invalid("공급자 참가자 식별자는 필수입니다.");
        }
        // 인증 주체를 하나로 확정해야 이후 기능에서 회원 권한이 게스트에게 잘못 적용되지 않는다.
        if (participantType == ParticipantType.GUEST) {
            if (guestSessionId == null || userId != null) {
                throw invalid("게스트 참가자는 게스트 세션 ID만 가져야 합니다.");
            }
        } else if (userId == null || guestSessionId != null) {
            throw invalid("회원 참가자는 사용자 ID만 가져야 합니다.");
        }
        return new ParticipantSession(
                id,
                meetingSessionId,
                meetingId,
                participantType,
                userId,
                guestSessionId,
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

    public UUID meetingSessionId() {
        return meetingSessionId;
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
}
