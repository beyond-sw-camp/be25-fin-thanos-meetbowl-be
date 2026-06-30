package com.meetbowl.application.meeting;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.meeting.MeetingExternalInvitee;
import com.meetbowl.domain.meeting.MeetingExternalInviteeRepositoryPort;

/** 회의 외부 초대 대상 목록을 회의 생성/수정 시 일괄 교체한다. */
@Service
public class MeetingExternalInviteeSyncService {

    private final MeetingExternalInviteeRepositoryPort meetingExternalInviteeRepositoryPort;

    public MeetingExternalInviteeSyncService(
            MeetingExternalInviteeRepositoryPort meetingExternalInviteeRepositoryPort) {
        this.meetingExternalInviteeRepositoryPort = meetingExternalInviteeRepositoryPort;
    }

    @Transactional
    public List<MeetingExternalInvitee> replace(
            UUID meetingId, List<ExternalInviteeCommand> externalInvitees) {
        meetingExternalInviteeRepositoryPort.deleteByMeetingId(meetingId);
        List<MeetingExternalInvitee> invitees =
                normalize(externalInvitees).stream()
                        .map(invitee -> MeetingExternalInvitee.create(meetingId, invitee.name(), invitee.email()))
                        .toList();
        if (invitees.isEmpty()) {
            return List.of();
        }
        return meetingExternalInviteeRepositoryPort.saveAll(invitees);
    }

    private List<ExternalInviteeCommand> normalize(List<ExternalInviteeCommand> externalInvitees) {
        if (externalInvitees == null || externalInvitees.isEmpty()) {
            return List.of();
        }
        return externalInvitees.stream()
                .filter(invitee -> invitee != null)
                .map(invitee -> new ExternalInviteeCommand(invitee.name(), invitee.email()))
                .distinct()
                .toList();
    }
}
