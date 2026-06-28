package com.meetbowl.domain.meeting;

import java.util.List;
import java.util.UUID;

/** 회의별 외부 초대 대상 저장소 경계다. */
public interface MeetingExternalInviteeRepositoryPort {

    List<MeetingExternalInvitee> saveAll(List<MeetingExternalInvitee> invitees);

    List<MeetingExternalInvitee> findByMeetingId(UUID meetingId);

    void deleteByMeetingId(UUID meetingId);
}
