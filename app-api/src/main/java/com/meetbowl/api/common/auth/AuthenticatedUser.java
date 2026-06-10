package com.meetbowl.api.common.auth;

import java.util.UUID;

/** 인증 방식이 바뀌어도 Controller와 UseCase의 사용자 계약이 함께 바뀌지 않도록 둔 중립 모델이다. */
public record AuthenticatedUser(
        UUID userId, UUID organizationId, AuthenticatedUserRole role, String displayName) {

    public boolean isAdmin() {
        return role == AuthenticatedUserRole.ADMIN;
    }

    public boolean isSystem() {
        return role == AuthenticatedUserRole.SYSTEM;
    }
}
