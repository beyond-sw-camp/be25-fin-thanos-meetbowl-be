package com.meetbowl.infrastructure.persistence.sampletask;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * sample-jpa 프로필에서만 샘플 JPA Entity/Repository를 스캔한다.
 * 기본 실행 DB에 샘플 테이블이 생기지 않도록 실제 기능 scan과 분리한다.
 */
@Profile("sample-jpa")
@EntityScan(basePackageClasses = SampleTaskEntity.class)
@EnableJpaRepositories(basePackageClasses = SpringDataSampleTaskRepository.class)
@Configuration
public class SampleTaskJpaConfig {
}
