package com.meetbowl.infrastructure.persistence;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;

import com.meetbowl.infrastructure.persistence.transcript.MeetingTranscriptSentenceEntity;
import com.meetbowl.infrastructure.persistence.video.VideoRoomEntity;

/** 화상회의 세션과 회의 원문 JPA Entity를 기본 실행 환경에 등록한다. */
@EntityScan(basePackageClasses = {VideoRoomEntity.class, MeetingTranscriptSentenceEntity.class})
@Configuration
public class VideoPersistenceJpaConfig {}
