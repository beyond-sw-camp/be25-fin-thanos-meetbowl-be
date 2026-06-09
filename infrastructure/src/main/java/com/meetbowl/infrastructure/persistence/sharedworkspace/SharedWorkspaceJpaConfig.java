package com.meetbowl.infrastructure.persistence.sharedworkspace;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackageClasses = SharedWorkspaceEntity.class)
@EnableJpaRepositories(basePackageClasses = SpringDataSharedWorkspaceRepository.class)
public class SharedWorkspaceJpaConfig {}
