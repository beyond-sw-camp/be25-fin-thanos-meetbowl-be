package com.meetbowl.infrastructure.persistence.transcript;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.transcript.MeetingTranscriptSentence;
import com.meetbowl.domain.transcript.MeetingTranscriptSentenceRepositoryPort;

/**
 * MeetingTranscriptSentenceRepositoryPort를 Spring Data JPA로 구현하는 Adapter다.
 *
 * <p>RabbitMQ 메시지 처리는 Application 계층에서 수행하고, 이 Adapter는 최종 문장의 저장/중복 확인/정렬 조회만 담당한다.
 */
@Repository
public class JpaMeetingTranscriptSentenceRepositoryAdapter
        implements MeetingTranscriptSentenceRepositoryPort {

    private final SpringDataMeetingTranscriptSentenceRepository repository;

    /** 최종 원문 문장 Entity의 저장과 조회를 담당하는 Spring Data Repository를 주입받는다. */
    public JpaMeetingTranscriptSentenceRepositoryAdapter(
            SpringDataMeetingTranscriptSentenceRepository repository) {
        this.repository = repository;
    }

    /** 검증된 Domain 문장을 Entity로 변환해 저장하고, 저장 결과를 다시 Domain으로 반환한다. */
    @Override
    public MeetingTranscriptSentence save(MeetingTranscriptSentence sentence) {
        return repository.save(MeetingTranscriptSentenceEntity.from(sentence)).toDomain();
    }

    /** RabbitMQ가 같은 이벤트를 재전달했는지 확인하기 위해 source_event_id unique 값을 조회한다. */
    @Override
    public boolean existsBySourceEventId(UUID sourceEventId) {
        return repository.existsBySourceEventId(sourceEventId);
    }

    /** 회의 전체 원문을 조립할 수 있도록 sequence_no 오름차순으로 문장 목록을 반환한다. */
    @Override
    public List<MeetingTranscriptSentence> findAllByMeetingIdOrderBySequenceNo(UUID meetingId) {
        return repository.findAllByMeetingIdOrderBySequenceNoAsc(meetingId).stream()
                .map(MeetingTranscriptSentenceEntity::toDomain)
                .toList();
    }
}
