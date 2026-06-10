package com.meetbowl.api.common.auth;

import java.util.UUID;

/** Controller가 받는 인증 사용자 정보다. JWT, 세션, 내부 토큰 파싱 결과는 이 타입으로 변환한 뒤 request attribute에 저장한다. */
public record AuthenticatedUser(
        UUID userId, UUID organizationId, AuthenticatedUserRole role, String displayName) {

    public boolean isAdmin() {
        return role == AuthenticatedUserRole.ADMIN;
    }

    public boolean isSystem() {
        return role == AuthenticatedUserRole.SYSTEM;
    }
}
