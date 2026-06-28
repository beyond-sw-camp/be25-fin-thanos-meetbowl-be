package com.meetbowl.infrastructure.persistence.meeting;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.meetbowl.domain.meeting.MeetingExternalInvitee;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** 회의 외부 초대 대상 영속 엔티티다. */
@Entity
@Table(
        name = "meeting_external_invitee",
        indexes = @Index(name = "idx_meeting_external_invitee_meeting", columnList = "meeting_id"))
public class MeetingExternalInviteeEntity extends BaseEntity {

    @Column(name = "meeting_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID meetingId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    protected MeetingExternalInviteeEntity() {}

    static MeetingExternalInviteeEntity from(MeetingExternalInvitee invitee) {
        MeetingExternalInviteeEntity entity = new MeetingExternalInviteeEntity();
        entity.setId(invitee.id());
        entity.meetingId = invitee.meetingId();
        entity.name = invitee.name();
        entity.email = invitee.email();
        return entity;
    }

    MeetingExternalInvitee toDomain() {
        return MeetingExternalInvitee.of(getId(), meetingId, name, email);
    }
}
