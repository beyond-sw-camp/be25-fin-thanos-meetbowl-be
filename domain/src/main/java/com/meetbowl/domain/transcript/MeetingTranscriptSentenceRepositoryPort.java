package com.meetbowl.domain.transcript;

import java.util.List;
import java.util.UUID;

/**
 * RabbitMQ로 수신한 최종 STT 문장을 멱등하게 저장하고 전체 회의 원문을 조회하기 위한 저장소 계약이다.
 *
 * <p>Application은 이벤트 중복 여부를 먼저 확인하고 저장하며, 조회 시에는 비동기 도착 순서가 아니라 sequenceNo를 기준으로 정렬된 결과를 사용한다.
 */
public interface MeetingTranscriptSentenceRepositoryPort {

    /** 검증된 최종 문장을 저장하고 DB 식별자가 반영된 도메인 모델을 반환한다. */
    MeetingTranscriptSentence save(MeetingTranscriptSentence sentence);

    /** 동일 RabbitMQ 이벤트가 이미 처리됐는지 확인해 적어도 한 번 전달로 인한 중복 저장을 방지한다. */
    boolean existsBySourceEventId(UUID sourceEventId);

    /** 한 회의의 최종 원문 문장을 sequenceNo 오름차순으로 반환한다. */
    List<MeetingTranscriptSentence> findAllByMeetingIdOrderBySequenceNo(UUID meetingId);
}
