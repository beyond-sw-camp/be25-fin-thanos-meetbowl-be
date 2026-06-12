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
 * 한 회의의 최종 STT 원문 segment 목록과 전체 조합 문자열을 조회한다.
 *
 * <p>화면에서는 segment 리스트로 순서/타임라인을 표현하고, 필요하면 `fullText`를 그대로 회의 원문 뷰의 기본 텍스트로 사용할 수 있다.
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

    public GetMeetingTranscriptResult execute(UUID meetingId, UUID requesterUserId, boolean isAdmin) {
        Meeting meeting =
                meetingRepositoryPort
                        .findById(meetingId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));
        validateAccess(meeting, requesterUserId, isAdmin);

        var segments =
                meetingTranscriptSegmentRepositoryPort.findAllByMeetingIdOrderBySequence(meetingId)
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

        String fullText =
                segments.stream()
                        .map(TranscriptSegmentResult::sourceText)
                        .filter(text -> text != null && !text.isBlank())
                        .map(String::trim)
                        .reduce((left, right) -> left + "\n" + right)
                        .orElse("");

        return new GetMeetingTranscriptResult(meetingId, fullText, segments);
    }

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
