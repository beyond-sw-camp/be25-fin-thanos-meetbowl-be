package com.meetbowl.api.common.auth;

/** 전역 역할과 회의별 역할을 섞으면 권한 범위가 과도하게 넓어지므로 시스템 수준 권한만 표현한다. */
public enum AuthenticatedUserRole {
    USER,
    ADMIN,
    SYSTEM
}
