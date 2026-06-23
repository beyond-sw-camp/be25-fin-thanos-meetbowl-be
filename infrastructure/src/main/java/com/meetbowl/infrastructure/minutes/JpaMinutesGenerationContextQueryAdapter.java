package com.meetbowl.infrastructure.minutes;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.meetbowl.application.minutes.MinutesGenerationContextQueryPort;
import com.meetbowl.application.minutes.MinutesGenerationContextResult;
import com.meetbowl.application.transcript.FinalTranscriptTextAssembler;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendee;
import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
import com.meetbowl.domain.transcript.MeetingTranscriptSegmentRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

/** 회의·참석자·사용자·Final Transcript를 AI 내부 Context 한 건으로 조립한다. */
@Repository
public class JpaMinutesGenerationContextQueryAdapter
        implements MinutesGenerationContextQueryPort {

    private final MeetingRepositoryPort meetingRepositoryPort;
    private final MeetingAttendeeRepositoryPort attendeeRepositoryPort;
    private final MeetingTranscriptSegmentRepositoryPort transcriptRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final FinalTranscriptTextAssembler transcriptTextAssembler;

    public JpaMinutesGenerationContextQueryAdapter(
            MeetingRepositoryPort meetingRepositoryPort,
            MeetingAttendeeRepositoryPort attendeeRepositoryPort,
            MeetingTranscriptSegmentRepositoryPort transcriptRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            FinalTranscriptTextAssembler transcriptTextAssembler) {
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.attendeeRepositoryPort = attendeeRepositoryPort;
        this.transcriptRepositoryPort = transcriptRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.transcriptTextAssembler = transcriptTextAssembler;
    }

    @Override
    public Optional<MinutesGenerationContextResult> findByMeetingId(UUID meetingId) {
        return meetingRepositoryPort.findById(meetingId).map(this::toContext);
    }

    private MinutesGenerationContextResult toContext(Meeting meeting) {
        var attendees = attendeeRepositoryPort.findByMeetingId(meeting.id());
        Map<UUID, User> users =
                attendees.stream()
                        .map(attendee -> userRepositoryPort.findById(attendee.userId()).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toMap(User::id, Function.identity(), (left, right) -> left));
        User host = userRepositoryPort.findById(meeting.hostUserId()).orElse(null);
        UUID organizationId = host == null ? null : host.affiliateId();
        UUID reviewerUserId =
                attendees.stream()
                        .filter(MeetingAttendee::reviewer)
                        .map(MeetingAttendee::userId)
                        .findFirst()
                        .orElse(null);
        var participants =
                attendees.stream()
                        .map(attendee -> users.get(attendee.userId()))
                        .filter(java.util.Objects::nonNull)
                        .map(
                                user ->
                                        new MinutesGenerationContextResult.Participant(
                                                user.id(), user.name(), null))
                        .toList();
        String rawTranscript =
                transcriptTextAssembler.assemble(
                        transcriptRepositoryPort.findAllByMeetingIdOrderBySequence(meeting.id()));
        return new MinutesGenerationContextResult(
                meeting.id(),
                organizationId,
                meeting.hostUserId(),
                reviewerUserId,
                meeting.title(),
                meeting.startedAt(),
                meeting.endedAt(),
                participants,
                rawTranscript);
    }
}
