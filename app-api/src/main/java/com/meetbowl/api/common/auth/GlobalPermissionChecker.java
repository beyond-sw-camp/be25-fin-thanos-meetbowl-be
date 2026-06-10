package com.meetbowl.api.common.auth;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** 전역 권한 검사만 담당해 API 진입 권한과 리소스별 업무 권한이 한 조건식에 섞이지 않게 한다. */
@Component
public class GlobalPermissionChecker {

    public void requireAdmin(AuthenticatedUser user) {
        requireAnyRole(user, EnumSet.of(AuthenticatedUserRole.ADMIN));
    }

    public void requireUserOrAdmin(AuthenticatedUser user) {
        requireAnyRole(user, EnumSet.of(AuthenticatedUserRole.USER, AuthenticatedUserRole.ADMIN));
    }

    public void requireSystem(AuthenticatedUser user) {
        requireAnyRole(user, EnumSet.of(AuthenticatedUserRole.SYSTEM));
    }

    public void rejectGuest(AuthenticatedUser user) {
        requireAuthenticated(user);
        if (user.isGuest()) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN);
        }
    }

    public void requireAnyRole(AuthenticatedUser user, Set<AuthenticatedUserRole> allowedRoles) {
        requireAuthenticated(user);
        if (!allowedRoles.contains(user.role())) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN);
        }
    }

    private void requireAuthenticated(AuthenticatedUser user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.COMMON_UNAUTHORIZED);
        }
    }
}
