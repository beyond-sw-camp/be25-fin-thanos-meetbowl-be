package com.meetbowl.infrastructure.persistence.meeting;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 회의 영속성 스캔 설정이다. meeting 패키지의 회의/참석자 Entity와 리포지토리를 기본 실행에 등록한다. 같은 패키지에 회의 관련 Entity가 추가되면 이 스캔
 * 범위에 함께 포함된다.
 */
@EntityScan(basePackageClasses = MeetingEntity.class)
@EnableJpaRepositories(basePackageClasses = SpringDataMeetingRepository.class)
@Configuration
public class MeetingJpaConfig {}
