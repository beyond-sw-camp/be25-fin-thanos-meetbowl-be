package com.meetbowl.infrastructure.persistence.transcript;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.transcript.MeetingTranscriptSegment;
import com.meetbowl.domain.transcript.MeetingTranscriptSegmentRepositoryPort;

/**
 * MeetingTranscriptSegmentRepositoryPort 인터페이스의 JPA 구현 어댑터입니다.
 *
 * <p>이 파일은 "최종 저장 직전 / 최종 조회 직전"의 영속성 경계다. SaveFinalTranscriptUseCase와 GetMeetingTranscriptUseCase는
 * 도메인 모델만 다루고, 실제 SQL/JPA 처리는 이 어댑터가 맡는다.
 *
 * <p>즉, finalized transcript 저장 흐름에서는 Listener -> SaveFinalTranscriptUseCase -> 현재 Adapter -> JPA
 * Repository -> MariaDB 순서로 내려간다.
 */
@Repository
public class JpaMeetingTranscriptSegmentRepositoryAdapter
        implements MeetingTranscriptSegmentRepositoryPort {

    private final SpringDataMeetingTranscriptSegmentRepository repository;

    public JpaMeetingTranscriptSegmentRepositoryAdapter(
            SpringDataMeetingTranscriptSegmentRepository repository) {
        this.repository = repository;
    }

    /**
     * [자막 세그먼트 저장]
     *
     * <p>이 메서드가 호출되는 시점에는 이미 상위 UseCase에서 - "이 데이터가 finalized segment인가" - "같은 sourceEventId가 이미
     * 저장됐는가" 검증이 끝난 상태다.
     *
     * <p>여기서는 그 결과를 엔티티로 바꿔 물리 DB에 반영하는 일만 한다.
     */
    @Override
    public MeetingTranscriptSegment save(MeetingTranscriptSegment segment) {
        // save 시점에는 이미 UseCase에서 "finalized segment만 저장" 정책과 멱등성 검증이 끝난 상태다.
        return repository.save(MeetingTranscriptSegmentEntity.from(segment)).toDomain();
    }

    /** [메시지 중복 확인] 동일한 원본 이벤트 ID(sourceEventId)가 이미 영속화되었는지 여부를 확인합니다. */
    @Override
    public boolean existsBySourceEventId(UUID sourceEventId) {
        return repository.existsBySourceEventId(sourceEventId);
    }

    /**
     * [회의별 전체 조회] 특정 회의의 모든 자막을 발화 순서(Sequence)에 맞춰 정렬된 상태로 조회합니다. 조회된 엔티티 리스트는 즉시 도메인 모델 리스트로 변환되어
     * 상위 계층에 전달됩니다.
     */
    @Override
    public List<MeetingTranscriptSegment> findAllByMeetingIdOrderBySequence(UUID meetingId) {
        // 회의 원문은 발화 순서가 의미의 핵심이므로 DB 조회 단계부터 sequence 정렬을 강제한다.
        return repository.findAllByMeetingIdOrderBySequenceAsc(meetingId).stream()
                .map(MeetingTranscriptSegmentEntity::toDomain)
                .toList();
    }
}
