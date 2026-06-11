package com.meetbowl.domain.sharedworkspace;

/** 공유 워크스페이스 멤버십 상태다. 활성(ACTIVE)과 추방·탈퇴된(REMOVED) 상태를 구분하며, 권한 계산은 ACTIVE만 인정한다. */
public enum SharedWorkspaceMemberStatus {
    ACTIVE,
    REMOVED
}
