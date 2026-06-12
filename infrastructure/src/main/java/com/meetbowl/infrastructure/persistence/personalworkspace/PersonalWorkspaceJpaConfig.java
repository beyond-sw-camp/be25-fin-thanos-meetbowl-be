package com.meetbowl.infrastructure.persistence.personalworkspace;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** 개인 워크스페이스 영속 패키지의 엔티티 스캔과 JPA 리포지토리 활성화를 묶는 설정이다. */
@EntityScan(basePackageClasses = PersonalWorkspaceCalendarEventEntity.class)
@EnableJpaRepositories(
        basePackageClasses = SpringDataPersonalWorkspaceCalendarEventRepository.class)
@Configuration
public class PersonalWorkspaceJpaConfig {}
