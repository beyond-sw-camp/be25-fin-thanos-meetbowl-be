package com.meetbowl.api.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Controller 메서드 파라미터에 붙여 인증 사용자 정보를 주입받는다. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {

    /** false면 인증 정보가 없어도 null을 주입한다. 공개 API에서 선택적으로 로그인 사용자를 참조할 때만 사용한다. */
    boolean required() default true;
}
