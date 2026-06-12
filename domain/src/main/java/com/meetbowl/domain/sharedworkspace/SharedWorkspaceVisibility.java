package com.meetbowl.domain.sharedworkspace;

/**
 * 공유 워크스페이스의 공개 범위다. 멤버만 접근(MEMBERS_ONLY)과 같은 조직 전 직원 공개(ORGANIZATION)를 구분한다. 기본값은 MEMBERS_ONLY다.
 */
public enum SharedWorkspaceVisibility {
    MEMBERS_ONLY,
    ORGANIZATION
}
