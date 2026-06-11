package com.meetbowl.infrastructure.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.meetbowl.common.time.UtcClock;
import com.meetbowl.infrastructure.messaging.RabbitMessagingProperties;

/** infrastructure 모듈의 Bean 등록 진입점이다. JPA, RabbitMQ, Redis, 외부 client 설정은 이 패키지 하위로 확장한다. */
@EnableJpaAuditing
@EnableConfigurationProperties(RabbitMessagingProperties.class)
@Configuration
public class InfrastructureConfig {

    /**
     * UseCase가 시간 기준을 직접 만들지 않고 주입받게 해, 운영에서는 UTC 기준을 강제하고 테스트에서는 고정 Clock으로 시각 의존 동작을 검증할 수 있게 한다.
     */
    @Bean
    public Clock clock() {
        return UtcClock.system();
    }
}
