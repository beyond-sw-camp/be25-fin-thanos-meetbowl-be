package com.meetbowl.infrastructure.persistence.personalworkspace;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackageClasses = PersonalWorkspaceCalendarEventEntity.class)
@EnableJpaRepositories(
        basePackageClasses = SpringDataPersonalWorkspaceCalendarEventRepository.class)
@Configuration
public class PersonalWorkspaceJpaConfig {}
