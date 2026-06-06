package com.meetbowl.api.common.auth;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** Controller 또는 API 조립 계층에서 전역 권한을 일관된 방식으로 검사한다. 리소스별 권한은 각 UseCase에서 별도로 검사한다. */
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
