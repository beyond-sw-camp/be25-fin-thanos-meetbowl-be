package com.meetbowl.infrastructure.persistence.meeting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;

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
    public List<Meeting> findByHostUserId(UUID hostUserId) {
        return springDataMeetingRepository.findByHostUserId(hostUserId).stream()
                .map(MeetingEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataMeetingRepository.deleteById(id);
    }
}
