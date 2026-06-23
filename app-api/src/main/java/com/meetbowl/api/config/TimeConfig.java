package com.meetbowl.api.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.meetbowl.common.time.UtcClock;

/** 서버 업무 시각을 UTC Clock Bean 하나로 통일해 UseCase가 시스템 시각 생성 방식에 직접 의존하지 않게 한다. */
@Configuration
public class TimeConfig {

    /** 운영에서는 시스템 UTC 시각을 사용하며, 테스트는 같은 생성자 계약에 고정 Clock을 주입한다. */
    @Bean
    Clock clock() {
        return UtcClock.system();
    }
}
