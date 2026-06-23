package com.meetbowl.api.common.auth;

/** 로그인 주체의 전역 권한이다. Host, Participant, Reviewer 같은 리소스별 권한은 각 UseCase에서 별도로 판정한다. */
public enum AuthenticatedUserRole {
    USER,
    ADMIN,
    SYSTEM
}
