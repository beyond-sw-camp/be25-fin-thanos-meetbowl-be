package com.meetbowl.application.minutes;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.minutes.MinutesGenerationContext;
import com.meetbowl.domain.minutes.MinutesGenerationContextQueryPort;

/** AI 서버가 실제 Final Transcript를 기반으로 초안을 만들 수 있도록 내부 Context를 조회한다. */
@Service
public class GetMinutesGenerationContextUseCase {

    private final MinutesGenerationContextQueryPort contextQueryPort;

    public GetMinutesGenerationContextUseCase(MinutesGenerationContextQueryPort contextQueryPort) {
        this.contextQueryPort = contextQueryPort;
    }

    @Transactional(readOnly = true)
    public MinutesGenerationContextResult execute(UUID meetingId) {
        MinutesGenerationContext context =
                contextQueryPort
                        .findByMeetingId(meetingId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));
        if (context.organizationId() == null) {
            throw new BusinessException(ErrorCode.MEETING_ORGANIZATION_REQUIRED);
        }
        if (context.reviewerUserId() == null) {
            throw new BusinessException(ErrorCode.MINUTES_REVIEWER_REQUIRED);
        }
        if (context.rawTranscript() == null || context.rawTranscript().isBlank()) {
            throw new BusinessException(
                    ErrorCode.MINUTES_TRANSCRIPT_REQUIRED, "회의록을 생성할 Final Transcript가 없습니다.");
        }
        return new MinutesGenerationContextResult(
                context.meetingId(),
                context.organizationId(),
                context.hostUserId(),
                context.reviewerUserId(),
                context.title(),
                context.startedAt(),
                context.endedAt(),
                context.participants().stream()
                        .map(
                                participant ->
                                        new MinutesGenerationContextResult.Participant(
                                                participant.userId(),
                                                participant.name(),
                                                participant.department()))
                        .toList(),
                context.rawTranscript());
    }
}
