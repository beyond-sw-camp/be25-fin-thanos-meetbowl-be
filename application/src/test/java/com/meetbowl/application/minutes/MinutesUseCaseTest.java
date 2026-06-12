package com.meetbowl.application.minutes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.document.DocumentIndexRequestedEvent;
import com.meetbowl.domain.document.DocumentIndexRequestedEventPort;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;
import com.meetbowl.domain.minutes.MinutesStatus;

/** 회의록 수정·승인 UseCase가 조직과 검토자 경계를 지키고 상태 전이를 저장하는지 검증한다. */
class MinutesUseCaseTest {

    @Test
    void reviewerCanReviseMinutes() {
        Fixture fixture = new Fixture();
        ReviseMinutesUseCase useCase = new ReviseMinutesUseCase(fixture.repository);

        MinutesResult result =
                useCase.execute(
                        new ReviseMinutesCommand(
                                fixture.meetingId,
                                fixture.reviewerUserId,
                                fixture.organizationId,
                                "수정된 회의 요약",
                                "수정된 회의록 본문"));

        assertEquals("IN_REVIEW", result.status());
        assertEquals("수정된 회의 요약", result.summary());
        assertEquals("수정된 회의록 본문", result.content());
        assertEquals(MinutesStatus.IN_REVIEW, fixture.repository.minutes.status());
    }

    @Test
    void nonReviewerCannotReviseMinutes() {
        Fixture fixture = new Fixture();
        ReviseMinutesUseCase useCase = new ReviseMinutesUseCase(fixture.repository);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new ReviseMinutesCommand(
                                                fixture.meetingId,
                                                UUID.randomUUID(),
                                                fixture.organizationId,
                                                "수정된 회의 요약",
                                                "수정된 회의록 본문")));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
    }

    @Test
    void otherOrganizationCannotApproveMinutes() {
        Fixture fixture = new Fixture();
        ApproveMinutesUseCase useCase =
                new ApproveMinutesUseCase(
                        fixture.repository,
                        fixture.eventPublisher,
                        fixture.textExtractor,
                        fixture.clock);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new ApproveMinutesCommand(
                                                fixture.meetingId,
                                                fixture.reviewerUserId,
                                                UUID.randomUUID())));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
    }

    @Test
    void reviewerCanApproveMinutes() {
        Fixture fixture = new Fixture();
        ApproveMinutesUseCase useCase =
                new ApproveMinutesUseCase(
                        fixture.repository,
                        fixture.eventPublisher,
                        fixture.textExtractor,
                        fixture.clock);

        MinutesResult result =
                useCase.execute(
                        new ApproveMinutesCommand(
                                fixture.meetingId, fixture.reviewerUserId, fixture.organizationId));

        assertEquals("APPROVED", result.status());
        assertEquals(fixture.now, result.approvedAt());
        assertEquals(fixture.reviewerUserId, result.reviewerUserId());
        assertEquals(result.minutesId(), fixture.eventPublisher.publishedEvent.documentId());
        assertEquals("MEETING_MINUTES", fixture.eventPublisher.publishedEvent.documentType());
        assertEquals(
                fixture.organizationId, fixture.eventPublisher.publishedEvent.organizationId());
        assertEquals(fixture.reviewerUserId, fixture.eventPublisher.publishedEvent.ownerUserId());
        assertEquals("회의록", fixture.eventPublisher.publishedEvent.title());
        assertEquals("회의록 본문", fixture.eventPublisher.publishedEvent.content());
        assertEquals(
                fixture.meetingId, fixture.eventPublisher.publishedEvent.metadata().meetingId());
        assertEquals(fixture.now, fixture.eventPublisher.publishedEvent.metadata().approvedAt());
        assertEquals(
                fixture.reviewerUserId, fixture.eventPublisher.publishedEvent.userIds().getFirst());
        assertEquals(0, fixture.eventPublisher.publishedEvent.departmentIds().size());
        assertEquals(0, fixture.eventPublisher.publishedEvent.sharedWorkspaceIds().size());
    }

    @Test
    void missingMinutesReturnsDomainError() {
        Fixture fixture = new Fixture();
        fixture.repository.minutes = null;
        ReviseMinutesUseCase useCase = new ReviseMinutesUseCase(fixture.repository);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new ReviseMinutesCommand(
                                                fixture.meetingId,
                                                fixture.reviewerUserId,
                                                fixture.organizationId,
                                                "수정된 회의 요약",
                                                "수정된 회의록 본문")));

        assertEquals(ErrorCode.MINUTES_NOT_FOUND, exception.errorCode());
    }

    @Test
    void getMinutesReturnsStoredDraft() {
        Fixture fixture = new Fixture();
        GetMinutesUseCase useCase = new GetMinutesUseCase(fixture.repository);

        MinutesResult result = useCase.get(fixture.meetingId, fixture.organizationId);

        assertEquals("DRAFT", result.status());
        assertEquals("회의 요약", result.summary());
        assertEquals("회의록 본문", result.content());
    }

    @Test
    void syncGeneratedMinutesCreatesDraftWhenMissing() {
        Fixture fixture = new Fixture();
        fixture.repository.minutes = null;
        SyncGeneratedMinutesUseCase useCase = new SyncGeneratedMinutesUseCase(fixture.repository);

        MinutesResult result =
                useCase.execute(
                        new SyncGeneratedMinutesCommand(
                                fixture.meetingId,
                                fixture.organizationId,
                                fixture.reviewerUserId,
                                "AI 요약",
                                "{\"type\":\"doc\"}",
                                "model",
                                "minutes-v1"));

        assertEquals("DRAFT", result.status());
        assertEquals("AI 요약", fixture.repository.minutes.summary());
        assertEquals("{\"type\":\"doc\"}", fixture.repository.minutes.content());
    }

    @Test
    void syncGeneratedMinutesReplacesUnapprovedDraft() {
        Fixture fixture = new Fixture();
        SyncGeneratedMinutesUseCase useCase = new SyncGeneratedMinutesUseCase(fixture.repository);

        MinutesResult result =
                useCase.execute(
                        new SyncGeneratedMinutesCommand(
                                fixture.meetingId,
                                fixture.organizationId,
                                fixture.reviewerUserId,
                                "재생성 요약",
                                "{\"type\":\"doc\",\"content\":[]}",
                                "model-v2",
                                "minutes-v2"));

        assertEquals("DRAFT", result.status());
        assertEquals("재생성 요약", fixture.repository.minutes.summary());
        assertEquals(MinutesStatus.DRAFT, fixture.repository.minutes.status());
    }

    @Test
    void syncGeneratedMinutesRejectsApprovedMinutes() {
        Fixture fixture = new Fixture();
        fixture.repository.minutes =
                fixture.repository.minutes.approve(fixture.reviewerUserId, fixture.now);
        SyncGeneratedMinutesUseCase useCase = new SyncGeneratedMinutesUseCase(fixture.repository);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new SyncGeneratedMinutesCommand(
                                                fixture.meetingId,
                                                fixture.organizationId,
                                                fixture.reviewerUserId,
                                                "재생성 요약",
                                                "{\"type\":\"doc\"}",
                                                "model-v2",
                                                "minutes-v2")));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    private static class Fixture {

        private final UUID meetingId = UUID.randomUUID();
        private final UUID organizationId = UUID.randomUUID();
        private final UUID reviewerUserId = UUID.randomUUID();
        private final Instant now = Instant.parse("2099-01-01T01:00:00Z");
        private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        private final MinutesContentTextExtractor textExtractor =
                new MinutesContentTextExtractor(new com.fasterxml.jackson.databind.ObjectMapper());
        private final FakeDocumentIndexRequestedEventPort eventPublisher =
                new FakeDocumentIndexRequestedEventPort();
        private final FakeMinutesRepository repository =
                new FakeMinutesRepository(
                        Minutes.of(
                                UUID.randomUUID(),
                                meetingId,
                                organizationId,
                                reviewerUserId,
                                MinutesStatus.DRAFT,
                                "회의 요약",
                                "회의록 본문",
                                "model",
                                "minutes-v1",
                                null,
                                null,
                                null));
    }

    private static class FakeDocumentIndexRequestedEventPort
            implements DocumentIndexRequestedEventPort {

        private DocumentIndexRequestedEvent publishedEvent;

        @Override
        public void publish(DocumentIndexRequestedEvent event) {
            publishedEvent = event;
        }
    }

    private static class FakeMinutesRepository implements MinutesRepositoryPort {

        private Minutes minutes;

        private FakeMinutesRepository(Minutes minutes) {
            this.minutes = minutes;
        }

        @Override
        public Minutes save(Minutes minutes) {
            this.minutes = minutes;
            return minutes;
        }

        @Override
        public Optional<Minutes> findById(UUID minutesId) {
            return Optional.ofNullable(minutes).filter(value -> value.id().equals(minutesId));
        }

        @Override
        public Optional<Minutes> findByMeetingId(UUID meetingId) {
            return Optional.ofNullable(minutes)
                    .filter(value -> value.meetingId().equals(meetingId));
        }

        @Override
        public boolean existsByMeetingId(UUID meetingId) {
            return findByMeetingId(meetingId).isPresent();
        }
    }
}
