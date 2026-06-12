package com.meetbowl.domain.sharedworkspace;

/** 공유 워크스페이스 멤버의 역할이다. 워크스페이스 관리 권한을 가진 소유자(OWNER)와 일반 참여자(MEMBER)를 구분한다. */
public enum SharedWorkspaceMemberRole {
    OWNER,
    MEMBER
}
