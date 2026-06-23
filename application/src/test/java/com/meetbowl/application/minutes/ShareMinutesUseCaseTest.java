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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.application.mail.SendMailCommand;
import com.meetbowl.application.mail.SendMailUseCase;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.AttendeeRole;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;
import com.meetbowl.domain.minutes.MinutesStatus;

class ShareMinutesUseCaseTest {

    @Test
    void sharesApprovedMinutesWithNonAttendee() {
        Fixture fixture = new Fixture();
        fixture.repository.minutes =
                fixture.repository.minutes.approve(fixture.reviewerUserId, fixture.now);
        ShareMinutesUseCase useCase =
                new ShareMinutesUseCase(
                        fixture.repository,
                        fixture.attendeeRepository,
                        fixture.sendMailUseCase,
                        fixture.metadataAssembler,
                        fixture.clock);

        MinutesResult result =
                useCase.execute(
                        new ShareMinutesCommand(
                                fixture.meetingId,
                                fixture.reviewerUserId,
                                fixture.organizationId,
                                List.of(fixture.nonAttendeeUserId),
                                "회의록 공유",
                                "본문",
                                fixture.idempotencyKey));

        assertEquals("SHARED", result.status());
        assertEquals(MinutesStatus.SHARED, fixture.repository.minutes.status());
        var captor = org.mockito.ArgumentCaptor.forClass(SendMailCommand.class);
        verify(fixture.sendMailUseCase).execute(captor.capture());
        assertEquals(List.of(fixture.nonAttendeeUserId), captor.getValue().recipientUserIds());
        assertEquals("MINUTES_SHARE", captor.getValue().bodyType());
        assertEquals("MEETING_MINUTES", captor.getValue().relatedResourceType());
    }

    @Test
    void rejectsAttendeeRecipient() {
        Fixture fixture = new Fixture();
        fixture.repository.minutes =
                fixture.repository.minutes.approve(fixture.reviewerUserId, fixture.now);
        ShareMinutesUseCase useCase =
                new ShareMinutesUseCase(
                        fixture.repository,
                        fixture.attendeeRepository,
                        fixture.sendMailUseCase,
                        fixture.metadataAssembler,
                        fixture.clock);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new ShareMinutesCommand(
                                                fixture.meetingId,
                                                fixture.reviewerUserId,
                                                fixture.organizationId,
                                                List.of(fixture.participantUserId),
                                                "회의록 공유",
                                                "본문",
                                                fixture.idempotencyKey)));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void rejectsUnapprovedMinutes() {
        Fixture fixture = new Fixture();
        ShareMinutesUseCase useCase =
                new ShareMinutesUseCase(
                        fixture.repository,
                        fixture.attendeeRepository,
                        fixture.sendMailUseCase,
                        fixture.metadataAssembler,
                        fixture.clock);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new ShareMinutesCommand(
                                                fixture.meetingId,
                                                fixture.reviewerUserId,
                                                fixture.organizationId,
                                                List.of(fixture.nonAttendeeUserId),
                                                "회의록 공유",
                                                "본문",
                                                fixture.idempotencyKey)));

        assertEquals(ErrorCode.MINUTES_REVIEW_REQUIRED, exception.errorCode());
    }

    private static class Fixture {
        private final UUID meetingId = UUID.randomUUID();
        private final UUID organizationId = UUID.randomUUID();
        private final UUID reviewerUserId = UUID.randomUUID();
        private final UUID participantUserId = UUID.randomUUID();
        private final UUID nonAttendeeUserId = UUID.randomUUID();
        private final UUID idempotencyKey = UUID.randomUUID();
        private final Instant now = Instant.parse("2099-01-01T01:00:00Z");
        private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
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
        private final FakeMeetingAttendeeRepository attendeeRepository =
                new FakeMeetingAttendeeRepository(meetingId, reviewerUserId, participantUserId);
        private final SendMailUseCase sendMailUseCase = mock(SendMailUseCase.class);
        private final MinutesMeetingMetadataAssembler metadataAssembler =
                mock(MinutesMeetingMetadataAssembler.class);

        private Fixture() {
            when(metadataAssembler.assemble(meetingId, organizationId, reviewerUserId))
                    .thenReturn(MinutesMeetingMetadata.empty(reviewerUserId));
        }
    }

    private static class FakeMeetingAttendeeRepository implements MeetingAttendeeRepositoryPort {
        private final UUID meetingId;
        private final List<UUID> attendeeUserIds;

        private FakeMeetingAttendeeRepository(UUID meetingId, UUID... attendeeUserIds) {
            this.meetingId = meetingId;
            this.attendeeUserIds = List.of(attendeeUserIds);
        }

        @Override
        public MeetingAttendee save(MeetingAttendee attendee) {
            return attendee;
        }

        @Override
        public List<MeetingAttendee> saveAll(List<MeetingAttendee> attendees) {
            return attendees;
        }

        @Override
        public List<MeetingAttendee> findByMeetingId(UUID meetingId) {
            if (!this.meetingId.equals(meetingId)) {
                return List.of();
            }
            List<MeetingAttendee> attendees = new ArrayList<>();
            for (UUID userId : attendeeUserIds) {
                attendees.add(MeetingAttendee.create(meetingId, userId, AttendeeRole.PARTICIPANT));
            }
            return attendees;
        }

        @Override
        public List<MeetingAttendee> findByMeetingIds(Collection<UUID> meetingIds) {
            return meetingIds.contains(meetingId) ? findByMeetingId(meetingId) : List.of();
        }

        @Override
        public List<MeetingAttendee> findByUserId(UUID userId) {
            return List.of();
        }

        @Override
        public Optional<UUID> findReviewerUserId(UUID meetingId) {
            return Optional.empty();
        }

        @Override
        public void deleteByMeetingId(UUID meetingId) {}
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
            return findByOrganizationId(organizationId);
        }
    }
}
