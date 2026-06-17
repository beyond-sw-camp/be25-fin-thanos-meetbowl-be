package com.meetbowl.infrastructure.persistence.notification;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 알림 영속 패키지의 Entity와 Spring Data repository 스캔 범위를 명시한다.
 *
 * <p>이 프로젝트는 전역 스캔 대신 영속 패키지마다 스캔 범위를 한정한다(도메인 모델이 JPA 모델로 오인되지 않게). 알림 도메인/엔티티는 먼저 머지됐지만 저장소가 없어 이
 * 설정이 빠져 있었으므로, 어댑터 구현과 함께 추가한다.
 */
@EntityScan(basePackageClasses = NotificationEntity.class)
@EnableJpaRepositories(basePackageClasses = SpringDataNotificationRepository.class)
@Configuration
public class NotificationJpaConfig {}
