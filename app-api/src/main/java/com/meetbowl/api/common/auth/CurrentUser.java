package com.meetbowl.api.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Controller가 토큰을 직접 파싱하지 않고 검증된 인증 주체만 선언하도록 하는 API 계층 경계다. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {

    /** 공개 API가 선택적으로 로그인 문맥을 사용할 때만 인증 부재를 허용한다. */
    boolean required() default true;
}
