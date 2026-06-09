package com.meetbowl.domain.transcript;

/**
 * 회의 원문 segment의 처리 상태를 나타낸다.
 *
 * <p>STREAMING은 STT 서버 메모리 버퍼나 임시 캐시에만 존재하고, MariaDB에는 FINAL 상태만 영구 저장한다.
 */
public enum MeetingTranscriptSegmentStatus {
    /** VAD, 무음, timeout 기준으로 아직 발화 종료가 확정되지 않은 중간 상태다. */
    STREAMING,

    /** 발화 종료가 확정되어 RabbitMQ로 전달되고 MariaDB에 저장 가능한 최종 상태다. */
    FINAL
}
