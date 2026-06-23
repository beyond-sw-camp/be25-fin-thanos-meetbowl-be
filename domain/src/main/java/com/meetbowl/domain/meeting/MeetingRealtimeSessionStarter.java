package com.meetbowl.domain.meeting;

import java.util.UUID;

/**
 * 회의 입장 시 실시간 보조 세션(STT 등)이 준비되도록 보장하는 Port다.
 *
 * <p>`meetbowl-be`는 내부 서버를 직접 구현하지 않고, 이 Port를 통해 "이 회의 room에 필요한 실시간 세션이 준비되었는지"만 요청한다.
 */
public interface MeetingRealtimeSessionStarter {

    void ensureStarted(UUID meetingId, UUID organizationId, String roomName);
}
