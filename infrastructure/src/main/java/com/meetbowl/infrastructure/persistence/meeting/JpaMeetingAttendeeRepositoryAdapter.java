package com.meetbowl.infrastructure.persistence.meeting;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;

/** MeetingAttendee domain port를 JPA로 구현하는 adapter다. Entity ↔ Domain 변환은 이 경계에서만 수행한다. */
@Repository
public class JpaMeetingAttendeeRepositoryAdapter implements MeetingAttendeeRepositoryPort {

    private final SpringDataMeetingAttendeeRepository springDataMeetingAttendeeRepository;

    public JpaMeetingAttendeeRepositoryAdapter(
            SpringDataMeetingAttendeeRepository springDataMeetingAttendeeRepository) {
        this.springDataMeetingAttendeeRepository = springDataMeetingAttendeeRepository;
    }

    @Override
    public MeetingAttendee save(MeetingAttendee attendee) {
        return springDataMeetingAttendeeRepository
                .save(MeetingAttendeeEntity.from(attendee))
                .toDomain();
    }

    @Override
    public List<MeetingAttendee> saveAll(List<MeetingAttendee> attendees) {
        List<MeetingAttendeeEntity> entities =
                attendees.stream().map(MeetingAttendeeEntity::from).toList();
        return springDataMeetingAttendeeRepository.saveAll(entities).stream()
                .map(MeetingAttendeeEntity::toDomain)
                .toList();
    }

    @Override
    public List<MeetingAttendee> findByMeetingId(UUID meetingId) {
        return springDataMeetingAttendeeRepository.findByMeetingId(meetingId).stream()
                .map(MeetingAttendeeEntity::toDomain)
                .toList();
    }

    @Override
    public List<MeetingAttendee> findByMeetingIds(Collection<UUID> meetingIds) {
        if (meetingIds.isEmpty()) {
            return List.of();
        }
        return springDataMeetingAttendeeRepository.findByMeetingIdIn(meetingIds).stream()
                .map(MeetingAttendeeEntity::toDomain)
                .toList();
    }

    @Override
    public List<MeetingAttendee> findByUserId(UUID userId) {
        return springDataMeetingAttendeeRepository.findByUserId(userId).stream()
                .map(MeetingAttendeeEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<UUID> findReviewerUserId(UUID meetingId) {
        return springDataMeetingAttendeeRepository
                .findByMeetingIdAndReviewerTrue(meetingId)
                .map(MeetingAttendeeEntity::toDomain)
                .map(MeetingAttendee::userId);
    }

    @Override
    public void deleteByMeetingId(UUID meetingId) {
        springDataMeetingAttendeeRepository.deleteByMeetingId(meetingId);
    }
}
