package com.meetbowl.infrastructure.persistence.meetingroom;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 회의실 기준정보 영속성 스캔 설정이다. 샘플 패키지와 달리 프로필 게이트 없이 기본 실행에 포함되어 실제 테이블/리포지토리를 등록한다. 같은 meetingroom 패키지에
 * 기준정보 Entity가 추가되면 이 스캔 범위에 함께 포함된다.
 */
@EntityScan(basePackageClasses = MeetingRoomEntity.class)
@EnableJpaRepositories(basePackageClasses = SpringDataMeetingRoomRepository.class)
@Configuration
public class MeetingRoomJpaConfig {}
