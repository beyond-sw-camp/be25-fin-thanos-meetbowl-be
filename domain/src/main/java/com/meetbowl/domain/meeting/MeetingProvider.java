package com.meetbowl.domain.meeting;

/**
 * 회의 미디어 세션을 실제로 제공하는 외부 기술 공급자를 구분한다.
 *
 * <p>도메인은 특정 SDK 구현에 직접 의존하지 않고 이 값과 공급자 방 식별자만 보관한다. 공급자가 추가되더라도 회의 세션의 공통 상태 모델은 유지하기 위한 경계값이다.
 */
public enum MeetingProvider {
    /** 현재 프로젝트에서 회의방과 미디어 스트림을 제공하는 LiveKit 공급자다. */
    LIVEKIT
}
