package com.meetbowl.infrastructure.persistence.transcript;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 최종 STT 문장 Entity의 중복 확인과 회의별 순차 조회를 제공한다.
 *
 * <p>Application이 직접 사용하지 않고 JPA Adapter 내부에서만 호출한다. 이벤트 멱등성과 전체 원문 조회 정렬을 위해 필요한 쿼리만 둔다.
 */
public interface SpringDataMeetingTranscriptSentenceRepository
        extends JpaRepository<MeetingTranscriptSentenceEntity, UUID> {

    /** source_event_id unique 제약과 함께 RabbitMQ 이벤트 중복 소비를 방지하는 조회다. */
    boolean existsBySourceEventId(UUID sourceEventId);

    /** 한 회의의 문장들을 STT 서버가 부여한 sequence_no 기준으로 정렬해 조회한다. */
    List<MeetingTranscriptSentenceEntity> findAllByMeetingIdOrderBySequenceNoAsc(UUID meetingId);
}
