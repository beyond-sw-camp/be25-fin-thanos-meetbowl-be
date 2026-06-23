package com.meetbowl.domain.minutes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** 회의록 도메인의 생성 검증과 상태 전이 규칙을 고정하는 테스트다. */
class MinutesTest {

    @Test
    void createDraftMinutes() {
        UUID meetingId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID reviewerUserId = UUID.randomUUID();

        Minutes minutes =
                Minutes.createDraft(
                        meetingId,
                        organizationId,
                        reviewerUserId,
                        "회의 요약",
                        "회의록 본문",
                        "llm-model-name",
                        "minutes-v1");

        assertEquals(null, minutes.id());
        assertEquals(meetingId, minutes.meetingId());
        assertEquals(organizationId, minutes.organizationId());
        assertEquals(reviewerUserId, minutes.reviewerUserId());
        assertEquals(MinutesStatus.DRAFT, minutes.status());
        assertEquals("회의 요약", minutes.summary());
        assertEquals("회의록 본문", minutes.content());
        assertEquals("llm-model-name", minutes.model());
        assertEquals("minutes-v1", minutes.promptVersion());
    }

    @Test
    void summaryMustNotBeBlank() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                Minutes.createDraft(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        " ",
                                        "회의록 본문",
                                        "model",
                                        "minutes-v1"));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void onlyReviewerCanApproveMinutes() {
        Minutes minutes = draftMinutes();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                minutes.approve(
                                        UUID.randomUUID(), Instant.parse("2099-01-01T01:00:00Z")));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
    }

    @Test
    void onlyReviewerCanReviseMinutes() {
        Minutes minutes = draftMinutes();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> minutes.revise("수정된 회의 요약", "수정된 회의록 본문", UUID.randomUUID()));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
    }

    @Test
    void reviseMinutesMovesToInReview() {
        UUID reviewerUserId = UUID.randomUUID();
        Minutes revised =
                draftMinutes(reviewerUserId).revise("수정된 회의 요약", "수정된 회의록 본문", reviewerUserId);

        assertEquals(MinutesStatus.IN_REVIEW, revised.status());
        assertEquals("수정된 회의 요약", revised.summary());
        assertEquals("수정된 회의록 본문", revised.content());
    }

    @Test
    void approvedMinutesCannotBeRevised() {
        UUID reviewerUserId = UUID.randomUUID();
        Minutes approved =
                draftMinutes(reviewerUserId)
                        .approve(reviewerUserId, Instant.parse("2099-01-01T01:00:00Z"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> approved.revise("수정된 회의 요약", "수정된 회의록 본문", reviewerUserId));

        assertEquals(ErrorCode.MINUTES_ALREADY_APPROVED, exception.errorCode());
    }

    @Test
    void approveMinutes() {
        UUID reviewerUserId = UUID.randomUUID();
        Minutes minutes = draftMinutes(reviewerUserId);
        Instant approvedAt = Instant.parse("2099-01-01T01:00:00Z");

        Minutes approved = minutes.approve(reviewerUserId, approvedAt);

        assertEquals(MinutesStatus.APPROVED, approved.status());
        assertEquals(reviewerUserId, approved.reviewerUserId());
        assertEquals(approvedAt, approved.approvedAt());
    }

    @Test
    void approvedMinutesCannotBeApprovedAgain() {
        UUID reviewerUserId = UUID.randomUUID();
        Minutes approved =
                draftMinutes(reviewerUserId)
                        .approve(reviewerUserId, Instant.parse("2099-01-01T01:00:00Z"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                approved.approve(
                                        reviewerUserId, Instant.parse("2099-01-01T02:00:00Z")));

        assertEquals(ErrorCode.MINUTES_ALREADY_APPROVED, exception.errorCode());
    }

    @Test
    void draftMinutesCannotBeShared() {
        Minutes minutes = draftMinutes();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> minutes.markShared(Instant.parse("2099-01-01T01:00:00Z")));

        assertEquals(ErrorCode.MINUTES_REVIEW_REQUIRED, exception.errorCode());
    }

    @Test
    void approvedMinutesCanBeShared() {
        UUID reviewerUserId = UUID.randomUUID();
        Instant sharedAt = Instant.parse("2099-01-01T02:00:00Z");
        Minutes approved =
                draftMinutes(reviewerUserId)
                        .approve(reviewerUserId, Instant.parse("2099-01-01T01:00:00Z"));

        Minutes shared = approved.markShared(sharedAt);

        assertEquals(MinutesStatus.SHARED, shared.status());
        assertEquals(sharedAt, shared.sharedAt());
    }

    private Minutes draftMinutes() {
        return draftMinutes(UUID.randomUUID());
    }

    private Minutes draftMinutes(UUID reviewerUserId) {
        return Minutes.createDraft(
                UUID.randomUUID(),
                UUID.randomUUID(),
                reviewerUserId,
                "회의 요약",
                "회의록 본문",
                "model",
                "minutes-v1");
    }
}
