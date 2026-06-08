package com.meetbowl.domain.minutes;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** 회의록 업무 규칙을 담는 도메인 모델이다. JPA나 API DTO에 의존하지 않고 상태 전이 규칙만 표현한다. */
public class Minutes {

    private final UUID id;
    private final UUID meetingId;
    private final UUID organizationId;
    private final UUID reviewerUserId;
    private final MinutesStatus status;
    private final String summary;
    private final String model;
    private final String promptVersion;
    private final UUID approvedByUserId;
    private final Instant approvedAt;
    private final Instant sharedAt;
    private final Instant deletionScheduledAt;

    private Minutes(
            UUID id,
            UUID meetingId,
            UUID organizationId,
            UUID reviewerUserId,
            MinutesStatus status,
            String summary,
            String model,
            String promptVersion,
            UUID approvedByUserId,
            Instant approvedAt,
            Instant sharedAt,
            Instant deletionScheduledAt) {
        validateRequiredIds(meetingId, organizationId, reviewerUserId);
        if (status == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의록 상태는 필수입니다.");
        }
        if (summary == null || summary.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의록 요약은 필수입니다.");
        }
        this.id = id;
        this.meetingId = meetingId;
        this.organizationId = organizationId;
        this.reviewerUserId = reviewerUserId;
        this.status = status;
        this.summary = summary;
        this.model = model == null ? "" : model;
        this.promptVersion = promptVersion == null ? "" : promptVersion;
        this.approvedByUserId = approvedByUserId;
        this.approvedAt = approvedAt;
        this.sharedAt = sharedAt;
        this.deletionScheduledAt = deletionScheduledAt;
    }

    /** AI 생성 결과 또는 수동 생성 결과를 검토 전 초안 상태로 만든다. */
    public static Minutes createDraft(
            UUID meetingId,
            UUID organizationId,
            UUID reviewerUserId,
            String summary,
            String model,
            String promptVersion) {
        return of(
                null,
                meetingId,
                organizationId,
                reviewerUserId,
                MinutesStatus.DRAFT,
                summary,
                model,
                promptVersion,
                null,
                null,
                null,
                null);
    }

    /** 저장소에서 조회한 값을 도메인 객체로 복원할 때 사용한다. */
    public static Minutes of(
            UUID id,
            UUID meetingId,
            UUID organizationId,
            UUID reviewerUserId,
            MinutesStatus status,
            String summary,
            String model,
            String promptVersion,
            UUID approvedByUserId,
            Instant approvedAt,
            Instant sharedAt,
            Instant deletionScheduledAt) {
        return new Minutes(
                id,
                meetingId,
                organizationId,
                reviewerUserId,
                status,
                summary,
                model,
                promptVersion,
                approvedByUserId,
                approvedAt,
                sharedAt,
                deletionScheduledAt);
    }

    public Minutes requestReview() {
        ensureNotApproved();
        return withStatus(
                MinutesStatus.IN_REVIEW,
                approvedByUserId,
                approvedAt,
                sharedAt,
                deletionScheduledAt);
    }

    public Minutes revise(String revisedSummary, UUID reviewerUserId) {
        ensureReviewer(reviewerUserId);
        ensureNotApproved();
        return of(
                id,
                meetingId,
                organizationId,
                this.reviewerUserId,
                MinutesStatus.IN_REVIEW,
                revisedSummary,
                model,
                promptVersion,
                approvedByUserId,
                approvedAt,
                sharedAt,
                deletionScheduledAt);
    }

    public Minutes approve(UUID reviewerUserId, Instant approvedAt) {
        ensureReviewer(reviewerUserId);
        ensureNotApproved();
        return withStatus(
                MinutesStatus.APPROVED,
                reviewerUserId,
                requireInstant(approvedAt, "회의록 승인 시각은 필수입니다."),
                sharedAt,
                deletionScheduledAt);
    }

    public Minutes markShared(Instant sharedAt) {
        if (status != MinutesStatus.APPROVED && status != MinutesStatus.SHARED) {
            throw new BusinessException(ErrorCode.MINUTES_REVIEW_REQUIRED, "승인 전 회의록은 공유할 수 없습니다.");
        }
        return withStatus(
                MinutesStatus.SHARED,
                approvedByUserId,
                approvedAt,
                requireInstant(sharedAt, "회의록 공유 시각은 필수입니다."),
                deletionScheduledAt);
    }

    public Minutes scheduleDeletion(Instant deletionScheduledAt) {
        return withStatus(
                MinutesStatus.DELETION_SCHEDULED,
                approvedByUserId,
                approvedAt,
                sharedAt,
                requireInstant(deletionScheduledAt, "회의록 삭제 예정 시각은 필수입니다."));
    }

    private Minutes withStatus(
            MinutesStatus nextStatus,
            UUID nextApprovedByUserId,
            Instant nextApprovedAt,
            Instant nextSharedAt,
            Instant nextDeletionScheduledAt) {
        return of(
                id,
                meetingId,
                organizationId,
                reviewerUserId,
                nextStatus,
                summary,
                model,
                promptVersion,
                nextApprovedByUserId,
                nextApprovedAt,
                nextSharedAt,
                nextDeletionScheduledAt);
    }

    /** 회의록 검토/승인 행위는 지정된 검토자만 수행할 수 있다. */
    private void ensureReviewer(UUID actorUserId) {
        if (!reviewerUserId.equals(actorUserId)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "회의록 검토자만 처리할 수 있습니다.");
        }
    }

    /** 승인 이후의 회의록은 재검토나 재승인으로 되돌리지 않는다. */
    private void ensureNotApproved() {
        if (status == MinutesStatus.APPROVED
                || status == MinutesStatus.SHARED
                || status == MinutesStatus.DELETION_SCHEDULED) {
            throw new BusinessException(ErrorCode.MINUTES_ALREADY_APPROVED, "이미 승인된 회의록입니다.");
        }
    }

    private static void validateRequiredIds(
            UUID meetingId, UUID organizationId, UUID reviewerUserId) {
        if (meetingId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의 ID는 필수입니다.");
        }
        if (organizationId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "조직 ID는 필수입니다.");
        }
        if (reviewerUserId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의록 검토자 ID는 필수입니다.");
        }
    }

    private static Instant requireInstant(Instant value, String message) {
        if (value == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
        }
        return value;
    }

    public UUID id() {
        return id;
    }

    public UUID meetingId() {
        return meetingId;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID reviewerUserId() {
        return reviewerUserId;
    }

    public MinutesStatus status() {
        return status;
    }

    public String summary() {
        return summary;
    }

    public String model() {
        return model;
    }

    public String promptVersion() {
        return promptVersion;
    }

    public UUID approvedByUserId() {
        return approvedByUserId;
    }

    public Instant approvedAt() {
        return approvedAt;
    }

    public Instant sharedAt() {
        return sharedAt;
    }

    public Instant deletionScheduledAt() {
        return deletionScheduledAt;
    }
}
