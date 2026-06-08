package com.meetbowl.domain.meeting;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * Meetbowl 회의 하나와 외부 미디어 공급자의 회의방 하나를 연결하는 도메인 모델이다.
 *
 * <p>일정과 제목 같은 회의 자체의 정보는 회의 도메인이 소유하고, 이 모델은 실시간 진행에 필요한 공급자 방 식별자와 생명주기만 관리한다. 공급자 SDK 객체를 저장하지
 * 않아 Domain이 Infrastructure에 의존하지 않도록 한다.
 */
public class MeetingSession {

    /** 영속화된 회의 진행 세션의 식별자다. 신규 생성 시에는 아직 발급되지 않아 null이다. */
    private final UUID id;

    /** 일정, 제목, 참석자 정보를 소유한 상위 회의 도메인의 식별자다. */
    private final UUID meetingId;

    /** 조직별 회의 세션 조회와 접근 범위 확인에 사용하는 조직 식별자다. */
    private final UUID organizationId;

    /** 회의 진행 권한을 가진 주최자 회원의 식별자다. */
    private final UUID hostUserId;

    /** 미디어 방을 실제로 생성하고 운영하는 외부 공급자다. */
    private final MeetingProvider provider;

    /** LiveKit room name처럼 공급자 API에서 회의방을 식별하는 값이다. */
    private final String providerRoomId;

    /** Meetbowl이 판단하는 회의 진행 세션의 현재 생명주기 상태다. */
    private final MeetingSessionStatus status;

    /** 참가자 입장을 허용하기 시작한 시각이며 아직 열리지 않았다면 null이다. */
    private final Instant openedAt;

    /** 실제 회의 진행이 시작된 시각이며 READY 상태에서는 null일 수 있다. */
    private final Instant startedAt;

    /** 회의 종료 시각이다. 종료 사유는 저장하지 않고 종료 시점만 추적한다. */
    private final Instant endedAt;

    private MeetingSession(
            UUID id,
            UUID meetingId,
            UUID organizationId,
            UUID hostUserId,
            MeetingProvider provider,
            String providerRoomId,
            MeetingSessionStatus status,
            Instant openedAt,
            Instant startedAt,
            Instant endedAt) {
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
    }

    /**
     * 상위 회의와 공급자 방을 연결하는 새 진행 세션을 생성한다.
     *
     * <p>생성 시점에는 참가자가 입장하지 않았으므로 상태를 READY로 고정하고 업무 시각은 비워 둔다. 영속 ID는 저장소가 발급한다.
     */
    public static MeetingSession create(
            UUID meetingId,
            UUID organizationId,
            UUID hostUserId,
            MeetingProvider provider,
            String providerRoomId) {
        return of(
                null,
                meetingId,
                organizationId,
                hostUserId,
                provider,
                providerRoomId,
                MeetingSessionStatus.READY,
                null,
                null,
                null);
    }

    /**
     * 저장소에서 읽은 값을 도메인 모델로 복원하면서 기본 불변조건을 다시 검증한다.
     *
     * <p>Entity가 잘못된 데이터를 그대로 Domain 밖으로 전달하지 않도록 신규 생성과 조회 복원 모두 이 팩토리를 통과한다.
     */
    public static MeetingSession of(
            UUID id,
            UUID meetingId,
            UUID organizationId,
            UUID hostUserId,
            MeetingProvider provider,
            String providerRoomId,
            MeetingSessionStatus status,
            Instant openedAt,
            Instant startedAt,
            Instant endedAt) {
        // 상위 회의, 조직, 주최자는 입장 권한과 데이터 소유 범위를 결정하므로 항상 존재해야 한다.
        require(meetingId, "회의 ID는 필수입니다.");
        require(organizationId, "조직 ID는 필수입니다.");
        require(hostUserId, "주최자 ID는 필수입니다.");
        require(provider, "화상회의 공급자는 필수입니다.");
        require(status, "화상회의방 상태는 필수입니다.");
        // 공급자 방 ID가 없으면 LiveKit 토큰 발급과 참가자 연결 대상을 결정할 수 없다.
        if (providerRoomId == null || providerRoomId.isBlank()) {
            throw invalid("공급자 회의방 ID는 필수입니다.");
        }
        // 두 시각이 모두 있을 때만 순서를 검증한다. 준비 중이거나 진행 중인 세션은 종료 시각이 없을 수 있다.
        if (startedAt != null && endedAt != null && endedAt.isBefore(startedAt)) {
            throw invalid("화상회의 종료 시각은 시작 시각보다 빠를 수 없습니다.");
        }
        return new MeetingSession(
                id,
                meetingId,
                organizationId,
                hostUserId,
                provider,
                providerRoomId,
                status,
                openedAt,
                startedAt,
                endedAt);
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

    public MeetingProvider provider() {
        return provider;
    }

    public String providerRoomId() {
        return providerRoomId;
    }

    public MeetingSessionStatus status() {
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
}
