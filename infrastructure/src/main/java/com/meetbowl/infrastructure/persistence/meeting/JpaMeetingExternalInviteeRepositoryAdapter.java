package com.meetbowl.infrastructure.persistence.meeting;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.meeting.MeetingExternalInvitee;
import com.meetbowl.domain.meeting.MeetingExternalInviteeRepositoryPort;

@Repository
public class JpaMeetingExternalInviteeRepositoryAdapter
        implements MeetingExternalInviteeRepositoryPort {

    private final SpringDataMeetingExternalInviteeRepository repository;

    public JpaMeetingExternalInviteeRepositoryAdapter(
            SpringDataMeetingExternalInviteeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MeetingExternalInvitee> saveAll(List<MeetingExternalInvitee> invitees) {
        return repository.saveAll(
                        invitees.stream().map(MeetingExternalInviteeEntity::from).toList())
                .stream()
                .map(MeetingExternalInviteeEntity::toDomain)
                .toList();
    }

    @Override
    public List<MeetingExternalInvitee> findByMeetingId(UUID meetingId) {
        return repository.findByMeetingIdOrderByCreatedAtAsc(meetingId).stream()
                .map(MeetingExternalInviteeEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteByMeetingId(UUID meetingId) {
        repository.deleteByMeetingId(meetingId);
    }
}
