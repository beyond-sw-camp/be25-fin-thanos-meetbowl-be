package com.meetbowl.domain.meeting;

import java.util.UUID;

/**
 * 회의 종료 시 실시간 보조 세션(STT 등)을 함께 종료시키는 Port다.
 *
 * <p>`meetbowl-be`는 회의 종료 authoritative state만 확정하고, 실제 세션 정리는 이 Port를 통해 내부 서버에 위임한다.
 */
public interface MeetingRealtimeSessionStopper {

    void stop(UUID meetingId);
}
