package com.meetbowl.application.minutes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.document.DocumentIndexRequestedEvent;
import com.meetbowl.domain.document.DocumentIndexRequestedEventPort;
import com.meetbowl.domain.document.MeetingMinutesAccessScopePort;
import com.meetbowl.application.mail.SendMailCommand;
import com.meetbowl.application.mail.SendMailUseCase;
import com.meetbowl.application.notification.DispatchNotificationUseCase;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;
import com.meetbowl.domain.minutes.MinutesStatus;

/** 회의록 수정·승인 UseCase가 조직과 검토자 경계를 지키고 상태 전이를 저장하는지 검증한다. */
class MinutesUseCaseTest {

    @Test
    void reviewerCanReviseMinutes() {
        Fixture fixture = new Fixture();
        ReviseMinutesUseCase useCase =
                new ReviseMinutesUseCase(fixture.repository, fixture.metadataAssembler);

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
        ReviseMinutesUseCase useCase =
                new ReviseMinutesUseCase(fixture.repository, fixture.metadataAssembler);

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
                        fixture.attendeeRepository,
                        fixture.eventPublisher,
                        fixture.textExtractor,
                        fixture.metadataAssembler,
                        fixture.sendMailUseCase,
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
                        fixture.attendeeRepository,
                        fixture.eventPublisher,
                        fixture.textExtractor,
                        fixture.metadataAssembler,
                        fixture.sendMailUseCase,
                        fixture.clock);

        MinutesResult result =
                useCase.execute(
                        new ApproveMinutesCommand(
                                fixture.meetingId, fixture.reviewerUserId, fixture.organizationId));

        assertEquals("SHARED", result.status());
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
                List.of(fixture.hostUserId, fixture.participantUserId, fixture.reviewerUserId),
                fixture.eventPublisher.publishedEvent.userIds());
        assertEquals(0, fixture.eventPublisher.publishedEvent.departmentIds().size());
        assertEquals(0, fixture.eventPublisher.publishedEvent.sharedWorkspaceIds().size());
        var mailCaptor = org.mockito.ArgumentCaptor.forClass(SendMailCommand.class);
        verify(fixture.sendMailUseCase).execute(mailCaptor.capture());
        assertEquals(fixture.reviewerUserId, mailCaptor.getValue().senderUserId());
        assertEquals(List.of(fixture.hostUserId, fixture.participantUserId), mailCaptor.getValue().recipientUserIds());
        assertEquals("MINUTES_SHARE", mailCaptor.getValue().bodyType());
        assertEquals("MEETING_MINUTES", mailCaptor.getValue().relatedResourceType());
        assertEquals(result.minutesId(), mailCaptor.getValue().relatedResourceId());
    }

    @Test
    void declinedAttendeeIsExcludedAndMissingReviewerIsIncludedWhenApprovingMinutes() {
        Fixture fixture = new Fixture();
        fixture.attendeeRepository.attendees = new ArrayList<>(List.of(fixture.hostUserId));
        ApproveMinutesUseCase useCase =
                new ApproveMinutesUseCase(
                        fixture.repository,
                        fixture.attendeeRepository,
                        fixture.eventPublisher,
                        fixture.textExtractor,
                        fixture.metadataAssembler,
                        fixture.sendMailUseCase,
                        fixture.clock);

        useCase.execute(
                new ApproveMinutesCommand(
                        fixture.meetingId, fixture.reviewerUserId, fixture.organizationId));

        assertEquals(
                List.of(fixture.hostUserId, fixture.reviewerUserId),
                fixture.eventPublisher.publishedEvent.userIds());
    }

    @Test
    void missingMinutesReturnsDomainError() {
        Fixture fixture = new Fixture();
        fixture.repository.minutes = null;
        ReviseMinutesUseCase useCase =
                new ReviseMinutesUseCase(fixture.repository, fixture.metadataAssembler);

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
        MinutesMeetingMetadataAssembler metadataAssembler = mock(MinutesMeetingMetadataAssembler.class);
        when(metadataAssembler.assemble(
                        fixture.meetingId, fixture.organizationId, fixture.reviewerUserId))
                .thenReturn(
                        new MinutesMeetingMetadata(
                                "주간 회의",
                                fixture.now.minusSeconds(3600),
                                fixture.now,
                                3,
                                fixture.reviewerUserId,
                                "검토자",
                                "프로덕트팀"));
        GetMinutesUseCase useCase = new GetMinutesUseCase(fixture.repository, metadataAssembler);

        MinutesResult result = useCase.get(fixture.meetingId, fixture.organizationId);

        assertEquals("DRAFT", result.status());
        assertEquals("회의 요약", result.summary());
        assertEquals("회의록 본문", result.content());
        assertEquals("주간 회의", result.meetingTitle());
        assertEquals("검토자", result.reviewerName());
    }

    @Test
    void syncGeneratedMinutesCreatesDraftWhenMissing() {
        Fixture fixture = new Fixture();
        fixture.repository.minutes = null;
        SyncGeneratedMinutesUseCase useCase =
                new SyncGeneratedMinutesUseCase(
                        fixture.repository, fixture.dispatchNotificationUseCase, fixture.eventRepository);

        MinutesResult result =
                useCase.execute(
                        new SyncGeneratedMinutesCommand(
                                UUID.randomUUID(),
                                fixture.meetingId,
                                fixture.organizationId,
                                fixture.reviewerUserId,
                                "AI 요약",
                                "{\"type\":\"doc\"}",
                                "model",
                                "minutes-v1"));

        assertEquals("IN_REVIEW", result.status());
        assertEquals("AI 요약", fixture.repository.minutes.summary());
        assertEquals("{\"type\":\"doc\"}", fixture.repository.minutes.content());
    }

    @Test
    void syncGeneratedMinutesReplacesUnapprovedDraft() {
        Fixture fixture = new Fixture();
        SyncGeneratedMinutesUseCase useCase =
                new SyncGeneratedMinutesUseCase(
                        fixture.repository, fixture.dispatchNotificationUseCase, fixture.eventRepository);

        MinutesResult result =
                useCase.execute(
                        new SyncGeneratedMinutesCommand(
                                UUID.randomUUID(),
                                fixture.meetingId,
                                fixture.organizationId,
                                fixture.reviewerUserId,
                                "재생성 요약",
                                "{\"type\":\"doc\",\"content\":[]}",
                                "model-v2",
                                "minutes-v2"));

        assertEquals("IN_REVIEW", result.status());
        assertEquals("재생성 요약", fixture.repository.minutes.summary());
        assertEquals(MinutesStatus.IN_REVIEW, fixture.repository.minutes.status());
    }

    @Test
    void syncGeneratedMinutesRejectsApprovedMinutes() {
        Fixture fixture = new Fixture();
        fixture.repository.minutes =
                fixture.repository.minutes.approve(fixture.reviewerUserId, fixture.now);
        SyncGeneratedMinutesUseCase useCase =
                new SyncGeneratedMinutesUseCase(
                        fixture.repository, fixture.dispatchNotificationUseCase, fixture.eventRepository);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new SyncGeneratedMinutesCommand(
                                                UUID.randomUUID(),
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
        private final UUID hostUserId = UUID.randomUUID();
        private final UUID participantUserId = UUID.randomUUID();
        private final Instant now = Instant.parse("2099-01-01T01:00:00Z");
        private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        private final MinutesContentTextExtractor textExtractor =
                new MinutesContentTextExtractor(new com.fasterxml.jackson.databind.ObjectMapper());
        private final FakeDocumentIndexRequestedEventPort eventPublisher =
                new FakeDocumentIndexRequestedEventPort();
        private final MinutesMeetingMetadataAssembler metadataAssembler = metadataAssembler();
        private final SendMailUseCase sendMailUseCase = mock(SendMailUseCase.class);
        private final FakeMeetingAttendeeRepository attendeeRepository =
                new FakeMeetingAttendeeRepository(
                        new ArrayList<>(List.of(hostUserId, participantUserId, reviewerUserId)));
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
        private final FakeMinutesGeneratedEventRepository eventRepository =
                new FakeMinutesGeneratedEventRepository();
        private final DispatchNotificationUseCase dispatchNotificationUseCase =
                mock(DispatchNotificationUseCase.class);

        private MinutesMeetingMetadataAssembler metadataAssembler() {
            MinutesMeetingMetadataAssembler assembler = mock(MinutesMeetingMetadataAssembler.class);
            when(assembler.assemble(meetingId, organizationId, reviewerUserId))
                    .thenReturn(
                            new MinutesMeetingMetadata(
                                    "주간 회의",
                                    now.minusSeconds(3600),
                                    now,
                                    3,
                                    reviewerUserId,
                                    "검토자",
                                    "프로덕트팀"));
            return assembler;
        }
    }

    private static class FakeMinutesGeneratedEventRepository
            implements com.meetbowl.domain.minutes.MinutesGeneratedEventRepositoryPort {
        private final java.util.Set<UUID> eventIds = new java.util.HashSet<>();

        @Override
        public boolean existsByEventId(UUID eventId) {
            return eventIds.contains(eventId);
        }

        @Override
        public void save(UUID eventId, UUID meetingId) {
            eventIds.add(eventId);
        }
    }

    private static class FakeMeetingAttendeeRepository implements MeetingMinutesAccessScopePort {

        private List<UUID> attendees;

        private FakeMeetingAttendeeRepository(List<UUID> attendees) {
            this.attendees = attendees;
        }

        @Override
        public List<UUID> findReadableUserIds(UUID meetingId) {
            return List.copyOf(attendees);
        }
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

        @Override
        public List<Minutes> findByOrganizationId(UUID organizationId) {
            return Optional.ofNullable(minutes)
                    .filter(value -> value.organizationId().equals(organizationId))
                    .stream()
                    .toList();
        }

        @Override
        public List<Minutes> searchByOrganizationId(UUID organizationId, String keyword) {
            return findByOrganizationId(organizationId).stream()
                    .filter(
                            value ->
                                    value.summary().contains(keyword)
                                            || value.content().contains(keyword))
                    .toList();
        }
    }
}
