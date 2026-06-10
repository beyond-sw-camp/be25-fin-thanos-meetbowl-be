package com.meetbowl.api.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Controller가 인증 프레임워크 타입 대신 프로젝트의 인증 주체만 의존하게 만드는 경계 표식이다. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {

    /** 공개 API가 로그인 사용자에게만 부가 동작을 제공해야 할 때에만 false를 사용한다. */
    boolean required() default true;
}
