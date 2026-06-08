package com.meetbowl.domain.mail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public class Mail {

    public static final int MAX_SUBJECT_LENGTH = 200;

    private final UUID id;
    private final UUID organizationId;
    private final UUID senderUserId;
    private final String subject;
    private final String body;
    private final MailType mailType;
    private final MailBodyType bodyType;
    private final RelatedResourceType relatedResourceType;
    private final UUID relatedResourceId;
    private final UUID idempotencyKey;
    private final Instant requestedAt;
    private final List<MailboxEntry> mailboxEntries;
    private final List<MailAttachment> attachments;
    private MailDeliveryStatus deliveryStatus;
    private Instant sentAt;
    private Instant failedAt;
    private String failureCode;

    private Mail(
            UUID id,
            UUID organizationId,
            UUID senderUserId,
            String subject,
            String body,
            MailType mailType,
            MailBodyType bodyType,
            RelatedResourceType relatedResourceType,
            UUID relatedResourceId,
            UUID idempotencyKey,
            MailDeliveryStatus deliveryStatus,
            Instant requestedAt,
            Instant sentAt,
            Instant failedAt,
            String failureCode,
            List<MailboxEntry> mailboxEntries,
            List<MailAttachment> attachments) {
        this.id = id;
        this.organizationId = requireNonNull(organizationId, "조직 ID는 필수입니다.");
        this.senderUserId = requireNonNull(senderUserId, "발신자 ID는 필수입니다.");
        this.subject = validateSubject(subject);
        this.body = requireText(body, "메일 본문은 필수입니다.");
        this.mailType = requireNonNull(mailType, "메일 유형은 필수입니다.");
        this.bodyType = requireNonNull(bodyType, "메일 본문 유형은 필수입니다.");
        validateRelatedResource(relatedResourceType, relatedResourceId);
        this.relatedResourceType = relatedResourceType;
        this.relatedResourceId = relatedResourceId;
        this.idempotencyKey = requireNonNull(idempotencyKey, "메일 멱등성 키는 필수입니다.");
        this.deliveryStatus = requireNonNull(deliveryStatus, "메일 발송 상태는 필수입니다.");
        this.requestedAt = requireNonNull(requestedAt, "메일 발송 요청 시각은 필수입니다.");
        validateDeliveryState(deliveryStatus, sentAt, failedAt, failureCode);
        validateDeliveryTime(this.requestedAt, sentAt, failedAt);
        this.sentAt = sentAt;
        this.failedAt = failedAt;
        this.failureCode = failureCode;
        this.mailboxEntries = new ArrayList<>(requireNonNull(mailboxEntries, "메일함 항목은 필수입니다."));
        this.attachments = new ArrayList<>(requireNonNull(attachments, "첨부파일 목록은 필수입니다."));
        validateMailboxEntries(this.mailboxEntries);
        validateAttachments(this.attachments);
    }

    public static Mail request(
            UUID organizationId,
            UUID senderUserId,
            List<UUID> recipientUserIds,
            String subject,
            String body,
            MailType mailType,
            MailBodyType bodyType,
            RelatedResourceType relatedResourceType,
            UUID relatedResourceId,
            UUID idempotencyKey,
            Instant requestedAt) {
        List<UUID> recipients = validateRecipients(recipientUserIds);
        List<MailboxEntry> mailboxEntries = new ArrayList<>();
        mailboxEntries.add(MailboxEntry.sent(senderUserId));
        recipients.forEach(recipientId -> mailboxEntries.add(MailboxEntry.inbox(recipientId)));

        return new Mail(
                null,
                organizationId,
                senderUserId,
                subject,
                body,
                mailType,
                bodyType,
                relatedResourceType,
                relatedResourceId,
                idempotencyKey,
                MailDeliveryStatus.REQUESTED,
                requestedAt,
                null,
                null,
                null,
                mailboxEntries,
                List.of());
    }

    public static Mail of(
            UUID id,
            UUID organizationId,
            UUID senderUserId,
            String subject,
            String body,
            MailType mailType,
            MailBodyType bodyType,
            RelatedResourceType relatedResourceType,
            UUID relatedResourceId,
            UUID idempotencyKey,
            MailDeliveryStatus deliveryStatus,
            Instant requestedAt,
            Instant sentAt,
            Instant failedAt,
            String failureCode,
            List<MailboxEntry> mailboxEntries,
            List<MailAttachment> attachments) {
        return new Mail(
                id,
                organizationId,
                senderUserId,
                subject,
                body,
                mailType,
                bodyType,
                relatedResourceType,
                relatedResourceId,
                idempotencyKey,
                deliveryStatus,
                requestedAt,
                sentAt,
                failedAt,
                failureCode,
                mailboxEntries,
                attachments);
    }

    public void markSent(Instant sentAt) {
        ensureRequested();
        Instant validatedSentAt = requireNonNull(sentAt, "메일 발송 완료 시각은 필수입니다.");
        ensureNotBeforeRequested(validatedSentAt, "메일 발송 완료 시각");
        this.deliveryStatus = MailDeliveryStatus.SENT;
        this.sentAt = validatedSentAt;
    }

    public void markFailed(Instant failedAt, String failureCode) {
        ensureRequested();
        Instant validatedFailedAt = requireNonNull(failedAt, "메일 발송 실패 시각은 필수입니다.");
        String validatedFailureCode = requireText(failureCode, "메일 발송 실패 코드는 필수입니다.");
        ensureNotBeforeRequested(validatedFailedAt, "메일 발송 실패 시각");
        this.deliveryStatus = MailDeliveryStatus.FAILED;
        this.failedAt = validatedFailedAt;
        this.failureCode = validatedFailureCode;
    }

    public void addAttachment(MailAttachment attachment) {
        requireNonNull(attachment, "첨부파일은 필수입니다.");
        boolean duplicated =
                attachments.stream()
                        .anyMatch(existing -> existing.objectKey().equals(attachment.objectKey()));
        if (duplicated) {
            throw invalid("동일한 object key의 첨부파일을 중복 등록할 수 없습니다.");
        }
        if (!senderUserId.equals(attachment.uploaderUserId())) {
            throw invalid("메일 발신자만 첨부파일을 등록할 수 있습니다.");
        }
        attachments.add(attachment);
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID senderUserId() {
        return senderUserId;
    }

    public String subject() {
        return subject;
    }

    public String body() {
        return body;
    }

    public MailType mailType() {
        return mailType;
    }

    public MailBodyType bodyType() {
        return bodyType;
    }

    public RelatedResourceType relatedResourceType() {
        return relatedResourceType;
    }

    public UUID relatedResourceId() {
        return relatedResourceId;
    }

    public UUID idempotencyKey() {
        return idempotencyKey;
    }

    public MailDeliveryStatus deliveryStatus() {
        return deliveryStatus;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public Instant sentAt() {
        return sentAt;
    }

    public Instant failedAt() {
        return failedAt;
    }

    public String failureCode() {
        return failureCode;
    }

    public List<MailboxEntry> mailboxEntries() {
        return List.copyOf(mailboxEntries);
    }

    public List<MailAttachment> attachments() {
        return List.copyOf(attachments);
    }

    public List<UUID> recipientUserIds() {
        return mailboxEntries.stream()
                .filter(entry -> entry.mailboxType() == MailboxType.INBOX)
                .map(MailboxEntry::ownerUserId)
                .toList();
    }

    private void ensureRequested() {
        if (deliveryStatus != MailDeliveryStatus.REQUESTED) {
            throw invalid("발송 요청 상태의 메일만 발송 결과를 변경할 수 있습니다.");
        }
    }

    private void ensureNotBeforeRequested(Instant resultAt, String fieldName) {
        if (resultAt.isBefore(requestedAt)) {
            throw invalid(fieldName + "은 발송 요청 시각보다 빠를 수 없습니다.");
        }
    }

    private static List<UUID> validateRecipients(List<UUID> recipientUserIds) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            throw invalid("메일 수신자는 한 명 이상이어야 합니다.");
        }
        if (recipientUserIds.stream().anyMatch(Objects::isNull)) {
            throw invalid("메일 수신자 ID는 필수입니다.");
        }
        if (new HashSet<>(recipientUserIds).size() != recipientUserIds.size()) {
            throw invalid("메일 수신자를 중복 지정할 수 없습니다.");
        }
        return List.copyOf(recipientUserIds);
    }

    private static String validateSubject(String subject) {
        String validated = requireText(subject, "메일 제목은 필수입니다.");
        if (validated.length() > MAX_SUBJECT_LENGTH) {
            throw invalid("메일 제목은 " + MAX_SUBJECT_LENGTH + "자를 초과할 수 없습니다.");
        }
        return validated;
    }

    private static void validateRelatedResource(
            RelatedResourceType relatedResourceType, UUID relatedResourceId) {
        if ((relatedResourceType == null) != (relatedResourceId == null)) {
            throw invalid("관련 리소스 유형과 ID는 함께 지정해야 합니다.");
        }
    }

    private static void validateDeliveryState(
            MailDeliveryStatus status, Instant sentAt, Instant failedAt, String failureCode) {
        if (status == MailDeliveryStatus.REQUESTED
                && (sentAt != null || failedAt != null || failureCode != null)) {
            throw invalid("발송 요청 상태에는 완료 또는 실패 정보를 지정할 수 없습니다.");
        }
        if (status == MailDeliveryStatus.SENT
                && (sentAt == null || failedAt != null || failureCode != null)) {
            throw invalid("발송 완료 상태의 시각 정보가 올바르지 않습니다.");
        }
        if (status == MailDeliveryStatus.FAILED
                && (failedAt == null
                        || failureCode == null
                        || failureCode.isBlank()
                        || sentAt != null)) {
            throw invalid("발송 실패 상태의 정보가 올바르지 않습니다.");
        }
    }

    private static void validateDeliveryTime(
            Instant requestedAt, Instant sentAt, Instant failedAt) {
        if (sentAt != null && sentAt.isBefore(requestedAt)) {
            throw invalid("메일 발송 완료 시각은 발송 요청 시각보다 빠를 수 없습니다.");
        }
        if (failedAt != null && failedAt.isBefore(requestedAt)) {
            throw invalid("메일 발송 실패 시각은 발송 요청 시각보다 빠를 수 없습니다.");
        }
    }

    private static void validateMailboxEntries(List<MailboxEntry> entries) {
        if (entries.isEmpty()) {
            throw invalid("메일에는 메일함 항목이 한 개 이상 필요합니다.");
        }
        Set<String> keys = new HashSet<>();
        for (MailboxEntry entry : entries) {
            requireNonNull(entry, "메일함 항목은 null일 수 없습니다.");
            String key = entry.ownerUserId() + ":" + entry.mailboxType();
            if (!keys.add(key)) {
                throw invalid("동일 사용자의 같은 유형 메일함 항목을 중복 등록할 수 없습니다.");
            }
        }
    }

    private static void validateAttachments(List<MailAttachment> attachments) {
        Set<String> objectKeys = new HashSet<>();
        for (MailAttachment attachment : attachments) {
            requireNonNull(attachment, "첨부파일은 null일 수 없습니다.");
            if (!objectKeys.add(attachment.objectKey())) {
                throw invalid("동일한 object key의 첨부파일을 중복 등록할 수 없습니다.");
            }
        }
    }

    private static <T> T requireNonNull(T value, String message) {
        if (Objects.isNull(value)) {
            throw invalid(message);
        }
        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw invalid(message);
        }
        return value;
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
    }
}
