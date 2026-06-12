package com.meetbowl.domain.personalworkspace;

/** 개인 캘린더 일정의 출처다. 사용자가 직접 만든 일정(PERSONAL)과 회의에서 파생된 일정(MEETING)을 구분해 직접 수정·삭제 허용 여부를 가른다. */
public enum CalendarEventSource {
    PERSONAL,
    MEETING
}
