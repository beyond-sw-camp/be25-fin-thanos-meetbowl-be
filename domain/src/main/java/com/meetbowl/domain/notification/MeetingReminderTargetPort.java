package com.meetbowl.domain.notification;

import java.time.Instant;
import java.util.List;

/**
 * 회의 리마인더 스케줄러가 발송 후보 회의를 조회하기 위한 읽기 포트다.
 *
 * <p>회의 도메인의 저장소 포트({@code MeetingRepositoryPort})를 수정하지 않고, 알림 기능이 필요로 하는 조회만 별도 계약으로 분리한다 — 회의 팀
 * 소유 포트에 메서드를 추가하지 않기 위함이다. 구현은 infrastructure adapter가 {@code MeetingEntity}에서 투영만 읽어 제공한다.
 */
public interface MeetingReminderTargetPort {

    /**
     * 예정 시작 시각이 ({@code from}, {@code to}] 구간에 드는 SCHEDULED 회의를 조회한다. 스케줄러는 현재 시각부터 알림 시간(분)의 최대
     * 범위까지를 구간으로 주어, 곧 시작할 회의만 가져온 뒤 참석자별 발송 시각을 계산한다.
     */
    List<MeetingReminderTarget> findScheduledStartingWithin(Instant from, Instant to);
}
