package com.meetbowl.infrastructure.persistence.mail;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;

/** 메일 영속성 엔티티의 탐색 범위를 infrastructure 패키지로 한정해 도메인 모델이 JPA 모델로 오인되지 않도록 한다. */
@EntityScan(basePackageClasses = MailEntity.class)
@Configuration
public class MailJpaConfig {}
