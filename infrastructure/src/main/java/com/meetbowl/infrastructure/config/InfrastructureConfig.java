package com.meetbowl.infrastructure.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * infrastructure 모듈의 Bean 등록 진입점이다.
 * JPA, RabbitMQ, Redis, 외부 client 설정은 이 패키지 하위로 확장한다.
 */
@EnableJpaAuditing
@EntityScan("com.meetbowl.infrastructure.persistence")
@EnableJpaRepositories("com.meetbowl.infrastructure.persistence")
@Configuration
public class InfrastructureConfig {
}
