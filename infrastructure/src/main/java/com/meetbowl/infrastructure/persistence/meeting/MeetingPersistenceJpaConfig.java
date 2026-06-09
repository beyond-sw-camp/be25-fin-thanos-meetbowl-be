package com.meetbowl.infrastructure.persistence.meeting;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.meetbowl.infrastructure.persistence.transcript.MeetingTranscriptSentenceEntity;
import com.meetbowl.infrastructure.persistence.transcript.SpringDataMeetingTranscriptSentenceRepository;

/**
 * 회의 원문 저장에 필요한 JPA 구성 요소를 등록한다.
 *
 * <p>meeting과 transcript는 패키지가 분리되어 있으므로 대표 Entity와 Repository 클래스를 명시해 두 패키지를 함께 스캔한다. sampletask의
 * sample-jpa 프로필 설정과 달리 실제 기능이므로 기본 실행 환경에서 항상 활성화한다.
 */
@EntityScan(basePackageClasses = {MeetingTranscriptSentenceEntity.class})
@EnableJpaRepositories(basePackageClasses = {SpringDataMeetingTranscriptSentenceRepository.class})
@Configuration
public class MeetingPersistenceJpaConfig {}
