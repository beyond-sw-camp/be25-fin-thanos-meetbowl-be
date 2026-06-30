package com.meetbowl.api.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * 현재 애플리케이션 역할에 따라 Bean 등록 여부를 제어한다.
 *
 * <p>{@code meetbowl.app.role} 또는 환경 변수 {@code MEETBOWL_APP_ROLE}를 읽고, 값이 없거나 알 수 없으면 기존 단일 배포와 같은
 * {@code all}로 처리한다.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(MeetbowlAppRoleCondition.class)
public @interface ConditionalOnMeetbowlAppRole {

    MeetbowlAppRole[] value();
}
