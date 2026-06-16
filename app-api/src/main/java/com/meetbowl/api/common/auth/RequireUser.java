package com.meetbowl.api.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/** 일반 로그인 사용자(USER)만 접근할 수 있는 API에 붙이는 전역 권한 어노테이션이다. Admin은 접근할 수 없다. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('USER')")
public @interface RequireUser {}
