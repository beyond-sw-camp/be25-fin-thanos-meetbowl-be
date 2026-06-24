package com.meetbowl.domain.meeting;

/** Outbox에서 조회한 meeting.ended 이벤트를 외부 메시지 브로커로 전달하는 Port다. */
public interface MeetingEndedEventPublisher {

    void publish(MeetingEndedEvent event);
}
