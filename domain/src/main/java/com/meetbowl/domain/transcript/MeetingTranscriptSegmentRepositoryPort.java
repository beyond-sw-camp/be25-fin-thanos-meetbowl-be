package com.meetbowl.domain.transcript;

import java.util.List;
import java.util.UUID;

/**
 * 최종 회의 원문 segment를 멱등하게 저장하고 전체 회의 원문을 조회하기 위한 저장소 계약이다.
 *
 * <p>Application은 RabbitMQ 이벤트 중복 여부를 먼저 확인하고 저장하며, 조회 시에는 비동기 도착 순서가 아니라 sequence 기준으로 정렬된 결과를
 * 사용한다.
 */
public interface MeetingTranscriptSegmentRepositoryPort {

    /** 검증된 FINAL segment를 저장하고 DB 식별자가 반영된 도메인 모델을 반환한다. */
    MeetingTranscriptSegment save(MeetingTranscriptSegment segment);

    /** 동일 RabbitMQ 이벤트가 이미 처리됐는지 확인해 적어도 한 번 전달로 인한 중복 저장을 방지한다. */
    boolean existsBySourceEventId(UUID sourceEventId);

    /** 한 회의의 최종 원문 segment를 sequence 오름차순으로 반환한다. */
    List<MeetingTranscriptSegment> findAllByMeetingIdOrderBySequence(UUID meetingId);
}
