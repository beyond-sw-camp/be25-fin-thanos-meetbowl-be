package com.meetbowl.api.common.auth;

/** 인증 생산자와 소비자가 서로 다른 키를 사용해 인증이 유실되는 것을 막기 위한 단일 정의다. */
public final class AuthenticatedUserAttributes {

    public static final String CURRENT_USER = AuthenticatedUser.class.getName() + ".CURRENT_USER";

    private AuthenticatedUserAttributes() {}
}
