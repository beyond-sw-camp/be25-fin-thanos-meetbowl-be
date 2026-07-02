package com.meetbowl.application.minutes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.AttendanceStatus;
import com.meetbowl.domain.meeting.AttendeeRole;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesFavorite;
import com.meetbowl.domain.minutes.MinutesFavoriteRepositoryPort;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;
import com.meetbowl.domain.minutes.MinutesStatus;

class MinutesFavoriteUseCaseTest {

    @Test
    void getMinutesListMarksFavorite() {
        Fixture fixture = new Fixture();
        fixture.favoriteRepository.save(
                MinutesFavorite.create(fixture.reviewerUserId, fixture.repository.minutes.id()));
        MinutesMeetingMetadataAssembler metadataAssembler =
                mock(MinutesMeetingMetadataAssembler.class);
        when(metadataAssembler.assemble(List.of(fixture.meetingId), fixture.organizationId))
                .thenReturn(
                        Map.of(
                                fixture.meetingId,
                                new MinutesMeetingMetadata(
                                        "주간 회의",
                                        null,
                                        null,
                                        3,
                                        fixture.reviewerUserId,
                                        "검토자",
                                        "프로덕트팀")));
        GetMinutesListUseCase useCase =
                new GetMinutesListUseCase(
                        fixture.repository,
                        fixture.favoriteRepository,
                        fixture.attendeeRepository,
                        metadataAssembler);

        List<MinutesListItemResult> results =
                useCase.execute(fixture.reviewerUserId, fixture.organizationId, null);

        assertEquals(1, results.size());
        assertEquals(fixture.repository.minutes.id(), results.get(0).minutesId());
        assertEquals(true, results.get(0).favorite());
        assertEquals("주간 회의", results.get(0).meetingTitle());
        assertEquals("검토자", results.get(0).reviewerName());
    }

    @Test
    void getMinutesListExcludesMeetingsWhereUserIsNotAttendee() {
        Fixture fixture = new Fixture();
        fixture.attendeeRepository.attendeeMeetingIds = List.of(UUID.randomUUID());
        GetMinutesListUseCase useCase =
                new GetMinutesListUseCase(
                        fixture.repository,
                        fixture.favoriteRepository,
                        fixture.attendeeRepository,
                        mock(MinutesMeetingMetadataAssembler.class));

        List<MinutesListItemResult> results =
                useCase.execute(fixture.reviewerUserId, fixture.organizationId, null);

        assertEquals(0, results.size());
    }

    @Test
    void addMinutesFavoriteIsIdempotent() {
        Fixture fixture = new Fixture();
        AddMinutesFavoriteUseCase useCase =
                new AddMinutesFavoriteUseCase(fixture.repository, fixture.favoriteRepository);

        useCase.execute(
                fixture.reviewerUserId, fixture.organizationId, fixture.repository.minutes.id());
        useCase.execute(
                fixture.reviewerUserId, fixture.organizationId, fixture.repository.minutes.id());

        assertEquals(1, fixture.favoriteRepository.favorites.size());
    }

    @Test
    void otherOrganizationCannotFavoriteMinutes() {
        Fixture fixture = new Fixture();
        AddMinutesFavoriteUseCase useCase =
                new AddMinutesFavoriteUseCase(fixture.repository, fixture.favoriteRepository);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        fixture.reviewerUserId,
                                        UUID.randomUUID(),
                                        fixture.repository.minutes.id()));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
    }

    private static class Fixture {

        private final UUID meetingId = UUID.randomUUID();
        private final UUID organizationId = UUID.randomUUID();
        private final UUID reviewerUserId = UUID.randomUUID();
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
        private final FakeMinutesFavoriteRepository favoriteRepository =
                new FakeMinutesFavoriteRepository();
        private final FakeMeetingAttendeeRepository attendeeRepository =
                new FakeMeetingAttendeeRepository(meetingId);
    }

    private static class FakeMeetingAttendeeRepository implements MeetingAttendeeRepositoryPort {

        private List<UUID> attendeeMeetingIds;

        private FakeMeetingAttendeeRepository(UUID meetingId) {
            this.attendeeMeetingIds = List.of(meetingId);
        }

        @Override
        public MeetingAttendee save(MeetingAttendee attendee) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<MeetingAttendee> saveAll(List<MeetingAttendee> attendees) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<MeetingAttendee> findByMeetingId(UUID meetingId) {
            return List.of();
        }

        @Override
        public List<MeetingAttendee> findByMeetingIds(java.util.Collection<UUID> meetingIds) {
            return List.of();
        }

        @Override
        public List<MeetingAttendee> findByUserId(UUID userId) {
            return attendeeMeetingIds.stream()
                    .map(
                            meetingId ->
                                    MeetingAttendee.of(
                                            UUID.randomUUID(),
                                            meetingId,
                                            userId,
                                            AttendeeRole.PARTICIPANT,
                                            AttendanceStatus.ACCEPTED))
                    .toList();
        }

        @Override
        public Optional<UUID> findReviewerUserId(UUID meetingId) {
            return Optional.empty();
        }

        @Override
        public void deleteByMeetingId(UUID meetingId) {
            throw new UnsupportedOperationException();
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
        public List<Minutes> findByOrganizationId(UUID organizationId) {
            return Optional.ofNullable(minutes)
                    .filter(value -> value.organizationId().equals(organizationId))
                    .stream()
                    .toList();
        }

        @Override
        public List<Minutes> findByOrganizationIdAndMeetingIds(
                UUID organizationId, Set<UUID> meetingIds) {
            return findByOrganizationId(organizationId).stream()
                    .filter(value -> meetingIds.contains(value.meetingId()))
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

        @Override
        public List<Minutes> searchByOrganizationIdAndMeetingIds(
                UUID organizationId, Set<UUID> meetingIds, String keyword) {
            return searchByOrganizationId(organizationId, keyword).stream()
                    .filter(value -> meetingIds.contains(value.meetingId()))
                    .toList();
        }

        @Override
        public boolean existsByMeetingId(UUID meetingId) {
            return findByMeetingId(meetingId).isPresent();
        }
    }

    private static class FakeMinutesFavoriteRepository implements MinutesFavoriteRepositoryPort {

        private final Map<UUID, MinutesFavorite> favorites = new LinkedHashMap<>();

        @Override
        public MinutesFavorite save(MinutesFavorite favorite) {
            MinutesFavorite saved =
                    MinutesFavorite.of(
                            favorite.id() == null ? UUID.randomUUID() : favorite.id(),
                            favorite.userId(),
                            favorite.minutesId());
            favorites.put(saved.id(), saved);
            return saved;
        }

        @Override
        public Optional<MinutesFavorite> findByUserIdAndMinutesId(UUID userId, UUID minutesId) {
            return favorites.values().stream()
                    .filter(
                            favorite ->
                                    favorite.userId().equals(userId)
                                            && favorite.minutesId().equals(minutesId))
                    .findFirst();
        }

        @Override
        public List<MinutesFavorite> findByUserId(UUID userId) {
            return favorites.values().stream()
                    .filter(favorite -> favorite.userId().equals(userId))
                    .toList();
        }

        @Override
        public void deleteByUserIdAndMinutesId(UUID userId, UUID minutesId) {
            findByUserIdAndMinutesId(userId, minutesId)
                    .ifPresent(favorite -> favorites.remove(favorite.id()));
        }
    }
}
