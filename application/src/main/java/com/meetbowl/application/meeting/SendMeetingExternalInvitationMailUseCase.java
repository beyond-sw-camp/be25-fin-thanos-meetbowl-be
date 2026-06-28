package com.meetbowl.application.meeting;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.mail.ExternalMailRecipient;
import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailBodyType;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailType;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.mail.RelatedResourceType;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingExternalInvitee;

/** 외부 초대가 있는 회의를 보낸 사람의 보낸함에 시스템 메일로 남긴다. */
@Service
public class SendMeetingExternalInvitationMailUseCase {

    private final MailRepositoryPort mailRepositoryPort;
    private final MailboxEntryRepositoryPort mailboxEntryRepositoryPort;
    private final Clock clock;
    private final String appBaseUrl;

    public SendMeetingExternalInvitationMailUseCase(
            MailRepositoryPort mailRepositoryPort,
            MailboxEntryRepositoryPort mailboxEntryRepositoryPort,
            Clock clock,
            @Value("${meetbowl.app.base-url:http://localhost:5173}") String appBaseUrl) {
        this.mailRepositoryPort = mailRepositoryPort;
        this.mailboxEntryRepositoryPort = mailboxEntryRepositoryPort;
        this.clock = clock;
        this.appBaseUrl = normalizeBaseUrl(appBaseUrl);
    }

    @Transactional
    public void execute(UUID organizationId, UUID senderUserId, Meeting meeting, List<MeetingExternalInvitee> invitees) {
        if (invitees == null || invitees.isEmpty()) {
            return;
        }

        Instant now = Instant.now(clock);
        Mail mail =
                Mail.createDraft(
                        organizationId,
                        senderUserId,
                        List.of(senderUserId),
                        invitees.stream()
                                .map(invitee -> new ExternalMailRecipient(invitee.name(), invitee.email()))
                                .toList(),
                        "[외부 회의 초대] " + meeting.title(),
                        buildBody(meeting, invitees),
                        MailType.SYSTEM,
                        MailBodyType.TEXT,
                        RelatedResourceType.MEETING,
                        meeting.id(),
                        UUID.randomUUID());
        mail.requestDelivery(now);
        mail.markSent(now);
        Mail saved = mailRepositoryPort.save(mail);
        mailboxEntryRepositoryPort.saveAll(List.of(MailboxEntry.sent(saved.id(), senderUserId)));
    }

    private String buildBody(Meeting meeting, List<MeetingExternalInvitee> invitees) {
        String recipients =
                invitees.stream()
                        .map(invitee -> "- " + invitee.name() + " <" + invitee.email() + ">")
                        .reduce((left, right) -> left + "\n" + right)
                        .orElse("");
        String guestEntryUrl = appBaseUrl + "/guest/meeting/" + meeting.id();
        return """
                회의명: %s
                시작 시각(UTC): %s
                종료 시각(UTC): %s
                게스트 입장 링크: %s

                외부 초대 대상
                %s
                """
                .formatted(
                        meeting.title(),
                        meeting.scheduledAt(),
                        meeting.scheduledEndAt(),
                        guestEntryUrl,
                        recipients);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:5173";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
