package com.meetbowl.infrastructure.persistence.transcript;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.transcript.MeetingTranscriptSegment;
import com.meetbowl.domain.transcript.MeetingTranscriptSegmentRepositoryPort;

/**
 * MeetingTranscriptSegmentRepositoryPort를 Spring Data JPA로 구현하는 Adapter다.
 *
 * <p>RabbitMQ 메시지 처리는 Application 계층에서 수행하고, 이 Adapter는 최종 segment의 저장, 중복 확인, 정렬 조회만 담당한다.
 */
@Repository
public class JpaMeetingTranscriptSegmentRepositoryAdapter
        implements MeetingTranscriptSegmentRepositoryPort {

    private final SpringDataMeetingTranscriptSegmentRepository repository;

    /** 최종 회의 원문 segment Entity의 저장과 조회를 담당하는 Spring Data Repository를 주입받는다. */
    public JpaMeetingTranscriptSegmentRepositoryAdapter(
            SpringDataMeetingTranscriptSegmentRepository repository) {
        this.repository = repository;
    }

    /** 검증된 FINAL segment를 Entity로 변환해 저장하고, 저장 결과를 다시 Domain으로 반환한다. */
    @Override
    public MeetingTranscriptSegment save(MeetingTranscriptSegment segment) {
        return repository.save(MeetingTranscriptSegmentEntity.from(segment)).toDomain();
    }

    /** RabbitMQ가 같은 이벤트를 재전달했는지 확인하기 위해 source_event_id unique 값을 조회한다. */
    @Override
    public boolean existsBySourceEventId(UUID sourceEventId) {
        return repository.existsBySourceEventId(sourceEventId);
    }

    /** 회의 전체 원문과 번역문을 재구성할 수 있도록 sequence_no 오름차순으로 segment 목록을 반환한다. */
    @Override
    public List<MeetingTranscriptSegment> findAllByMeetingIdOrderBySequence(UUID meetingId) {
        return repository.findAllByMeetingIdOrderBySequenceAsc(meetingId).stream()
                .map(MeetingTranscriptSegmentEntity::toDomain)
                .toList();
    }
}
