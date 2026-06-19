package com.meetbowl.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 주기 작업(@Scheduled) 활성화 설정이다.
 *
 * <p>현재는 알림 SSE 연결 유지를 위한 하트비트(NotificationSseService)가 사용한다. 회의 리마인더/회의록 검토 지연 같은 발송 스케줄러는 발송 기준
 * 시각 설계가 확정되면 이 활성화를 그대로 활용해 추가한다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
