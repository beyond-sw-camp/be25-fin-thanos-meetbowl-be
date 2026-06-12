package com.meetbowl.application.minutes;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;
import com.meetbowl.domain.minutes.MinutesStatus;

/** AI 서버가 발행한 회의록 초안을 저장하거나 재생성 결과로 교체한다. */
@Service
public class SyncGeneratedMinutesUseCase {

    private final MinutesRepositoryPort minutesRepositoryPort;

    public SyncGeneratedMinutesUseCase(MinutesRepositoryPort minutesRepositoryPort) {
        this.minutesRepositoryPort = minutesRepositoryPort;
    }

    @Transactional
    public MinutesResult execute(SyncGeneratedMinutesCommand command) {
        Minutes draft =
                minutesRepositoryPort
                        .findByMeetingId(command.meetingId())
                        .map(existing -> replaceDraft(existing, command))
                        .orElseGet(
                                () ->
                                        Minutes.createDraft(
                                                command.meetingId(),
                                                command.organizationId(),
                                                command.reviewerUserId(),
                                                command.summary(),
                                                command.content(),
                                                command.model(),
                                                command.promptVersion()));
        return MinutesResult.from(minutesRepositoryPort.save(draft));
    }

    private Minutes replaceDraft(Minutes existing, SyncGeneratedMinutesCommand command) {
        if (!existing.organizationId().equals(command.organizationId())) {
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT,
                    "같은 회의 ID로 다른 조직의 회의록 초안을 저장할 수 없습니다.");
        }
        if (existing.status() == MinutesStatus.APPROVED
                || existing.status() == MinutesStatus.SHARED
                || existing.status() == MinutesStatus.DELETION_SCHEDULED) {
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT,
                    "이미 승인된 회의록은 AI 초안으로 덮어쓸 수 없습니다.");
        }
        return Minutes.of(
                existing.id(),
                command.meetingId(),
                command.organizationId(),
                command.reviewerUserId(),
                MinutesStatus.DRAFT,
                command.summary(),
                command.content(),
                command.model(),
                command.promptVersion(),
                null,
                null,
                null);
    }
}
