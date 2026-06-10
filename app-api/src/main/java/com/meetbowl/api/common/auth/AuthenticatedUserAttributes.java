package com.meetbowl.api.common.auth;

/** 인증 필터와 CurrentUserArgumentResolver가 공유하는 request attribute 이름이다. */
public final class AuthenticatedUserAttributes {

    public static final String CURRENT_USER = AuthenticatedUser.class.getName() + ".CURRENT_USER";

    private AuthenticatedUserAttributes() {}
}
