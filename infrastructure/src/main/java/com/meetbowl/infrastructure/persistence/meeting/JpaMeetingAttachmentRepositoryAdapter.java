package com.meetbowl.infrastructure.persistence.meeting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.meeting.MeetingAttachment;
import com.meetbowl.domain.meeting.MeetingAttachmentRepositoryPort;

/** MeetingAttachment domain port를 JPA로 구현하는 adapter다. Entity ↔ Domain 변환은 이 경계에서만 수행한다. */
@Repository
public class JpaMeetingAttachmentRepositoryAdapter implements MeetingAttachmentRepositoryPort {

    private final SpringDataMeetingAttachmentRepository springDataMeetingAttachmentRepository;

    public JpaMeetingAttachmentRepositoryAdapter(
            SpringDataMeetingAttachmentRepository springDataMeetingAttachmentRepository) {
        this.springDataMeetingAttachmentRepository = springDataMeetingAttachmentRepository;
    }

    @Override
    public MeetingAttachment save(MeetingAttachment attachment) {
        return springDataMeetingAttachmentRepository
                .save(MeetingAttachmentEntity.from(attachment))
                .toDomain();
    }

    @Override
    public Optional<MeetingAttachment> findById(UUID id) {
        return springDataMeetingAttachmentRepository
                .findById(id)
                .map(MeetingAttachmentEntity::toDomain);
    }

    @Override
    public List<MeetingAttachment> findByMeetingId(UUID meetingId) {
        return springDataMeetingAttachmentRepository.findByMeetingId(meetingId).stream()
                .map(MeetingAttachmentEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataMeetingAttachmentRepository.deleteById(id);
    }
}
