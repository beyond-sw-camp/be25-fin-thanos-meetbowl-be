package com.meetbowl.infrastructure.persistence.transcript;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 최종 회의 원문 segment Entity의 중복 확인과 회의별 순차 조회를 제공한다.
 *
 * <p>Application이 직접 사용하지 않고 JPA Adapter 내부에서만 호출한다. 이벤트 멱등성과 전체 원문/번역 조회 정렬에 필요한 쿼리만 둔다.
 */
public interface SpringDataMeetingTranscriptSegmentRepository
        extends JpaRepository<MeetingTranscriptSegmentEntity, UUID> {

    /** source_event_id unique 제약과 함께 RabbitMQ 이벤트 중복 소비를 방지하는 조회다. */
    boolean existsBySourceEventId(UUID sourceEventId);

    /** 한 회의의 최종 segment를 STT 서버가 부여한 sequence_no 기준으로 정렬해 조회한다. */
    List<MeetingTranscriptSegmentEntity> findAllByMeetingIdOrderBySequenceAsc(UUID meetingId);
}
