package com.meetbowl.api.config;

import java.util.Arrays;

/**
 * meetbowl-be 실행 역할 구분이다.
 *
 * <p>기본값은 기존 단일 배포와 동일한 {@code all}이며, 분리 배포 시에만 {@code api} / {@code worker}를 명시한다.
 */
public enum MeetbowlAppRole {
    ALL,
    API,
    WORKER;

    public static MeetbowlAppRole from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return ALL;
        }
        return Arrays.stream(values())
                .filter(role -> role.name().equalsIgnoreCase(rawValue.trim()))
                .findFirst()
                .orElse(ALL);
    }
}
