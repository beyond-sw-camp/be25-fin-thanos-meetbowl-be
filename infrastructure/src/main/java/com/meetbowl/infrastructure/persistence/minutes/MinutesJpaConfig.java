package com.meetbowl.infrastructure.persistence.minutes;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** minutes persistence 패키지의 Entity와 Spring Data repository 스캔 범위를 명시한다. */
@EntityScan(basePackageClasses = MinutesEntity.class)
@EnableJpaRepositories(basePackageClasses = SpringDataMinutesRepository.class)
@Configuration
public class MinutesJpaConfig {}
