package com.meetbowl.domain.mail;

/** 메일이 가리키는 관련 업무 리소스의 종류다. 회의·회의록·워크스페이스로 연결해 메일에서 원본 자료로 이동할 수 있게 한다. */
public enum RelatedResourceType {
    MEETING,
    MEETING_MINUTES,
    WORKSPACE
}
