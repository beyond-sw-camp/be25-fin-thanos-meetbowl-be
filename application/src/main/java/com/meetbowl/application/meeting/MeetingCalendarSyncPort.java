package com.meetbowl.application.meeting;

import java.util.UUID;

/**
 * 회의 일정 변경을 개인 워크스페이스 캘린더로 전달하는 포트(회의 모듈의 출력 계약)다.
 *
 * <p>회의 모듈은 이 포트만 호출하고, 실제 MEETING source 일정의 생성/갱신/삭제는 개인 워크스페이스 모듈 구현체가 책임진다.
 *
 * <p><b>구현 담당(개인 워크스페이스/캘린더 모듈):</b> 이 인터페이스를 구현한 {@code @Component}(어댑터)를 등록하면, 회의 생성/수정/취소 시 자동으로
 * 호출된다. 구현체가 아직 없으면(미머지 상태) 회의 모듈이 {@code ObjectProvider.ifAvailable}로 호출을 건너뛰므로, 회의 기능 자체는 영향을 받지
 * 않고 구현체가 들어오는 순간부터 일정 투영이 동작한다. (회의 측은 코드 변경 불필요.)
 */
public interface MeetingCalendarSyncPort {

    /**
     * 회의 생성/수정 시 호출된다. 참석자별 개인 캘린더에 MEETING source 일정을 투영한다.
     *
     * <p><b>멱등(upsert) 구현 필수:</b> 같은 {@code meetingId}로 다시 호출돼도 중복 일정이 생기면 안 된다. 참석자별로 해당 회의 일정이
     * 없으면 생성, 있으면 시간/제목을 갱신한다. (회의 수정 시 이 메서드가 재호출됨)
     */
    void syncFromMeeting(MeetingCalendarSyncCommand command);

    /** 회의 취소 시 호출된다. 해당 {@code meetingId}로 투영된 모든 MEETING source 일정을 제거한다. */
    void removeMeetingEvents(UUID meetingId);
}
