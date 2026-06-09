package com.meetbowl.domain.meeting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 회의 첨부파일 메타데이터 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface MeetingAttachmentRepositoryPort {

    MeetingAttachment save(MeetingAttachment attachment);

    Optional<MeetingAttachment> findById(UUID id);

    List<MeetingAttachment> findByMeetingId(UUID meetingId);

    void deleteById(UUID id);
}
