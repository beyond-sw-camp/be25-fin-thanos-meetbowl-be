package com.meetbowl.application.transcript;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.transcript.MeetingTranscriptSegmentRepositoryPort;

/**
 * 특정 회의의 전체 STT 원문을 조회하는 유스케이스입니다.
 *
 * <p>이 유스케이스는 "이미 DB에 저장이 끝난 finalized segment 목록"을 읽는다.
 * 즉, 실시간 자막 채널을 직접 읽지 않고 영속화된 결과만 조회한다.
 *
 * <p>따라서 화면에 자막이 보였더라도, 이 유스케이스에서 비어 있다면
 * 중간에 FINALIZED 생성 / RabbitMQ 발행 / BE 저장 중 하나가 빠진 것이다.
 *
 * <p>개별 발화 세그먼트 목록과 이를 하나로 결합한 전체 텍스트를 함께 제공하며, 요청자의 조회 권한을 엄격히 검증합니다.
 */
@Service
@Transactional(readOnly = true)
public class GetMeetingTranscriptUseCase {

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort;
    private final MeetingTranscriptSegmentRepositoryPort meetingTranscriptSegmentRepositoryPort;

    public GetMeetingTranscriptUseCase(
            MeetingRepositoryPort meetingRepositoryPort,
            MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort,
            MeetingTranscriptSegmentRepositoryPort meetingTranscriptSegmentRepositoryPort) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.meetingAttendeeRepositoryPort = meetingAttendeeRepositoryPort;
        this.meetingTranscriptSegmentRepositoryPort = meetingTranscriptSegmentRepositoryPort;
    }

    /**
     * [회의 원문 조회 실행]
     * 1. 회의 존재 여부 확인
     * 2. 접근 권한 검증 (관리자, 주최자, 또는 참석자 여부 확인)
     * 3. 데이터 로드: 해당 회의의 모든 세그먼트를 순서(Sequence)대로 조회
     * 4. 텍스트 결합: 조회된 세그먼트들의 텍스트를 개행 문자로 연결하여 전체 원문 생성
     *
     * @param meetingId 조회할 회의 식별자
     * @param requesterUserId 요청자 식별자
     * @param isAdmin 관리자 여부
     * @return 전체 원문 및 세그먼트 리스트를 포함한 결과 객체
     */
    public GetMeetingTranscriptResult execute(
            UUID meetingId, UUID requesterUserId, boolean isAdmin) {
        Meeting meeting =
                meetingRepositoryPort
                        .findById(meetingId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));
        
        // 비즈니스 보안 정책에 따른 접근 권한 확인
        validateAccess(meeting, requesterUserId, isAdmin);

        /**
         * 저장된 원문은 "finalized segment 목록"이다.
         *
         * 예를 들어 화면에서는
         * - "오늘"
         * - "오늘 배포"
         * - "오늘 배포 일정"
         * 처럼 STREAMING이 여러 번 흔들릴 수 있지만,
         * 여기서는 그중 마지막에 확정된 FINALIZED 문장만 내려간다.
         */
        var segments =
                meetingTranscriptSegmentRepositoryPort
                        .findAllByMeetingIdOrderBySequence(meetingId)
                        .stream()
                        .map(
                                segment ->
                                        new TranscriptSegmentResult(
                                                segment.segmentId(),
                                                segment.sequence(),
                                                segment.sourceLanguage().name(),
                                                segment.sourceText(),
                                                segment.startedAtMs(),
                                                segment.endedAtMs()))
                        .toList();

        /**
         * 회의 원문 화면에서는 세그먼트 목록 자체도 필요하지만,
         * 회의록 초안 생성이나 복사용 전체 텍스트도 자주 쓰이므로 여기서 개행 기준으로 한 번 더 합친다.
         */
        String fullText =
                segments.stream()
                        .map(TranscriptSegmentResult::sourceText)
                        .filter(text -> text != null && !text.isBlank())
                        .map(String::trim)
                        .reduce((left, right) -> left + "\n" + right)
                        .orElse("");

        return new GetMeetingTranscriptResult(meetingId, fullText, segments);
    }

    /**
     * [접근 권한 검증]
     * - 시스템 관리자는 모든 회의 원문 조회 가능
     * - 회의 주최자(Host)는 본인 회의 원문 조회 가능
     * - 일반 참석자는 본인이 승인된 회의의 원문 조회 가능
     * - 그 외의 경우 403 Forbidden 예외 발생
     */
    private void validateAccess(Meeting meeting, UUID requesterUserId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (requesterUserId == null) {
            throw new BusinessException(ErrorCode.COMMON_UNAUTHORIZED);
        }
        if (meeting.isHostedBy(requesterUserId)) {
            return;
        }
        boolean isParticipant =
                meetingAttendeeRepositoryPort.findByMeetingId(meeting.id()).stream()
                        .anyMatch(attendee -> attendee.userId().equals(requesterUserId));
        if (!isParticipant) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN);
        }
    }
}
