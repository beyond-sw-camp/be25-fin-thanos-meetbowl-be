package com.meetbowl.domain.meeting;

/**
 * 참가자 한 명의 회의 입장 요청부터 연결 종료까지의 상태를 나타낸다.
 *
 * <p>퇴장 시각과 사유는 별도 저장하지 않으므로, 현재 참가자가 회의에 연결되어 있는지는 이 상태값을 기준으로 판단한다.
 */
public enum ParticipantSessionStatus {
    /** 입장 권한은 확인됐지만 미디어 세션 연결이 완료되지 않은 상태다. */
    JOIN_REQUESTED,

    /** LiveKit 연결이 완료되어 회의에 참여 중인 상태다. */
    JOINED,

    /** 참가자가 정상적으로 회의에서 나간 상태다. */
    LEFT,

    /** 네트워크 또는 클라이언트 연결이 비정상적으로 끊어진 상태다. */
    DISCONNECTED,

    /** 주최자 또는 운영 정책에 의해 회의에서 제거된 상태다. */
    KICKED
}
