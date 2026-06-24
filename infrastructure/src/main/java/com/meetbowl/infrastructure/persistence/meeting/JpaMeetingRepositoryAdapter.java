package com.meetbowl.infrastructure.persistence.meeting;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.meeting.AttendeeConflict;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.meeting.MeetingStatus;

/** Meeting domain port를 JPA로 구현하는 adapter다. Entity ↔ Domain 변환은 이 경계에서만 수행한다. */
@Repository
public class JpaMeetingRepositoryAdapter implements MeetingRepositoryPort {

    private final SpringDataMeetingRepository springDataMeetingRepository;

    public JpaMeetingRepositoryAdapter(SpringDataMeetingRepository springDataMeetingRepository) {
        this.springDataMeetingRepository = springDataMeetingRepository;
    }

    @Override
    public Meeting save(Meeting meeting) {
        return springDataMeetingRepository.save(MeetingEntity.from(meeting)).toDomain();
    }

    @Override
    public Optional<Meeting> findById(UUID id) {
        return springDataMeetingRepository.findById(id).map(MeetingEntity::toDomain);
    }

    @Override
    public List<Meeting> findByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataMeetingRepository.findAllById(ids).stream()
                .map(MeetingEntity::toDomain)
                .toList();
    }

    @Override
    public List<Meeting> findByHostUserId(UUID hostUserId) {
        return springDataMeetingRepository.findByHostUserId(hostUserId).stream()
                .map(MeetingEntity::toDomain)
                .toList();
    }

    @Override
    public List<Meeting> findActiveRoomOverlaps(
            UUID meetingRoomId, Instant scheduledStartAt, Instant scheduledEndAt) {
        return springDataMeetingRepository
                .findActiveRoomOverlaps(
                        meetingRoomId,
                        List.of(MeetingStatus.SCHEDULED, MeetingStatus.IN_PROGRESS),
                        scheduledStartAt,
                        scheduledEndAt)
                .stream()
                .map(MeetingEntity::toDomain)
                .toList();
    }

    @Override
    public List<Meeting> findActiveOverlapsInRooms(
            List<UUID> meetingRoomIds, Instant from, Instant to) {
        if (meetingRoomIds.isEmpty()) {
            return List.of();
        }
        return springDataMeetingRepository
                .findActiveOverlapsInRooms(
                        meetingRoomIds,
                        List.of(MeetingStatus.SCHEDULED, MeetingStatus.IN_PROGRESS),
                        from,
                        to)
                .stream()
                .map(MeetingEntity::toDomain)
                .toList();
    }

    @Override
    public List<Meeting> findNonCancelledRoomMeetingsOverlapping(Instant from, Instant to) {
        return springDataMeetingRepository
                .findRoomMeetingsOverlappingExcludingStatus(MeetingStatus.CANCELLED, from, to)
                .stream()
                .map(MeetingEntity::toDomain)
                .toList();
    }

    @Override
    public List<AttendeeConflict> findActiveByAttendees(
            Collection<UUID> userIds, Instant from, Instant to, UUID excludeMeetingId) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        // 활성 상태 집합은 회의실 겹침과 동일하게 adapter가 주입한다(SCHEDULED/IN_PROGRESS). 쿼리가 AttendeeConflict로
        // 직접 투영하므로 별도 매핑이 필요 없다.
        return springDataMeetingRepository.findActiveByAttendees(
                userIds,
                List.of(MeetingStatus.SCHEDULED, MeetingStatus.IN_PROGRESS),
                from,
                to,
                excludeMeetingId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataMeetingRepository.deleteById(id);
    }
}
