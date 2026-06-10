package com.meetbowl.infrastructure.persistence.community;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 커뮤니티 영속성 스캔 설정이다. community 패키지의 게시글/댓글/좋아요/익명매핑 Entity와 리포지토리를 기본 실행에 등록한다. 같은 패키지에 커뮤니티 관련 Entity가
 * 추가되면 이 스캔 범위에 함께 포함된다.
 */
@EntityScan(basePackageClasses = PostEntity.class)
@EnableJpaRepositories(basePackageClasses = SpringDataPostRepository.class)
@Configuration
public class CommunityJpaConfig {}