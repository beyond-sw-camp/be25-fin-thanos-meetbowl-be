package com.meetbowl.application.minutes;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.application.mail.SendMailCommand;
import com.meetbowl.application.mail.SendMailUseCase;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.document.DocumentIndexRequestedEvent;
import com.meetbowl.domain.document.DocumentIndexRequestedEventPort;
import com.meetbowl.domain.document.MeetingMinutesAccessScopePort;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;

/** 지정 검토자가 회의록을 최종 승인하고 AI 검색용 문서 색인을 요청하는 UseCase다. */
@Service
public class ApproveMinutesUseCase {

    private static final String MEETING_MINUTES_DOCUMENT_TYPE = "MEETING_MINUTES";
    private static final String TEMPORARY_MINUTES_TITLE = "회의록";

    private final MinutesRepositoryPort minutesRepositoryPort;
    private final MeetingMinutesAccessScopePort meetingMinutesAccessScopePort;
    private final DocumentIndexRequestedEventPort documentIndexRequestedEventPort;
    private final MinutesContentTextExtractor minutesContentTextExtractor;
    private final MinutesMeetingMetadataAssembler metadataAssembler;
    private final SendMailUseCase sendMailUseCase;
    private final Clock clock;

    /**
     * 운영과 테스트 모두 동일한 생성자 계약을 사용한다.
     *
     * <p>운영에서는 app-api의 UTC Clock Bean을, 테스트에서는 고정 Clock을 주입해 승인 시각을 결정적으로 검증한다.
     */
    public ApproveMinutesUseCase(
            MinutesRepositoryPort minutesRepositoryPort,
            MeetingMinutesAccessScopePort meetingMinutesAccessScopePort,
            DocumentIndexRequestedEventPort documentIndexRequestedEventPort,
            MinutesContentTextExtractor minutesContentTextExtractor,
            MinutesMeetingMetadataAssembler metadataAssembler,
            SendMailUseCase sendMailUseCase,
            Clock clock) {
        this.minutesRepositoryPort = minutesRepositoryPort;
        this.meetingMinutesAccessScopePort = meetingMinutesAccessScopePort;
        this.documentIndexRequestedEventPort = documentIndexRequestedEventPort;
        this.minutesContentTextExtractor = minutesContentTextExtractor;
        this.metadataAssembler = metadataAssembler;
        this.sendMailUseCase = sendMailUseCase;
        this.clock = clock;
    }

    @Transactional
    public MinutesResult execute(ApproveMinutesCommand command) {
        Minutes minutes = findByMeetingId(command.meetingId());

        // 조직 경계를 먼저 확인한 뒤 도메인에서 지정 검토자 일치와 현재 상태를 검증한다.
        // 승인 저장과 이벤트 발행을 같은 UseCase 흐름에서 처리해 발행 실패 시 승인 요청도 실패로 반환한다.
        MinutesAccessValidator.ensureSameOrganization(minutes, command.actorOrganizationId());

        // 클라이언트 시각은 신뢰하지 않고 서버 UTC Clock으로 승인 시각을 확정한다.
        Minutes approved = minutes.approve(command.actorUserId(), Instant.now(clock));
        Minutes saved = minutesRepositoryPort.save(approved);

        // 회의록 검색 결과가 실제 열람 가능 사용자에게만 노출되도록 참석자 기준 접근 범위를 함께 색인한다.
        // 참석자 데이터가 불완전해도 지정 검토자는 승인한 회의록을 계속 열람할 수 있어야 한다.
        List<UUID> readableUserIds =
                meetingMinutesAccessScopePort.findReadableUserIds(saved.meetingId());
        if (!readableUserIds.contains(saved.reviewerUserId())) {
            readableUserIds =
                    java.util.stream.Stream.concat(
                                    readableUserIds.stream(),
                                    java.util.stream.Stream.of(saved.reviewerUserId()))
                            .distinct()
                            .toList();
        }
        documentIndexRequestedEventPort.publish(
                new DocumentIndexRequestedEvent(
                        saved.id(),
                        MEETING_MINUTES_DOCUMENT_TYPE,
                        saved.organizationId(),
                        saved.reviewerUserId(),
                        TEMPORARY_MINUTES_TITLE,
                        minutesContentTextExtractor.extract(saved.content()),
                        new DocumentIndexRequestedEvent.Metadata(
                                saved.meetingId(),
                                saved.approvedAt(),
                                null,
                                null,
                                null,
                                null,
                                null),
                        readableUserIds,
                        List.of(),
                        List.of()));
        shareWithParticipants(saved, readableUserIds);
        Minutes shared = minutesRepositoryPort.save(saved.markShared(Instant.now(clock)));
        return MinutesResult.from(
                shared,
                metadataAssembler.assemble(
                        shared.meetingId(), shared.organizationId(), shared.reviewerUserId()));
    }

    private void shareWithParticipants(Minutes minutes, List<UUID> readableUserIds) {
        List<UUID> recipients =
                readableUserIds.stream()
                        .filter(userId -> !userId.equals(minutes.reviewerUserId()))
                        .distinct()
                        .toList();
        if (recipients.isEmpty()) {
            return;
        }
        sendMailUseCase.execute(
                new SendMailCommand(
                        minutes.organizationId(),
                        minutes.reviewerUserId(),
                        recipients,
                        subject(minutes),
                        body(minutes),
                        "MINUTES_SHARE",
                        "MEETING_MINUTES",
                        minutes.id(),
                        deterministicIdempotencyKey("minutes.auto-share:" + minutes.id())));
    }

    private String subject(Minutes minutes) {
        MinutesMeetingMetadata metadata =
                metadataAssembler.assemble(
                        minutes.meetingId(), minutes.organizationId(), minutes.reviewerUserId());
        String title = metadata.title() == null ? "회의록" : metadata.title();
        return "[회의록 공유] " + title;
    }

    private String body(Minutes minutes) {
        return "승인된 회의록을 공유드립니다.\n\n[AI 요약]\n"
                + minutes.summary()
                + "\n\n[회의록 본문]\n"
                + minutesContentTextExtractor.extract(minutes.content());
    }

    private UUID deterministicIdempotencyKey(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    /** 회의 테이블이 없는 현재 단계에서는 회의록의 meetingId unique 관계를 조회 기준으로 사용한다. */
    private Minutes findByMeetingId(UUID meetingId) {
        return minutesRepositoryPort
                .findByMeetingId(meetingId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.MINUTES_NOT_FOUND, "회의록을 찾을 수 없습니다."));
    }
}
