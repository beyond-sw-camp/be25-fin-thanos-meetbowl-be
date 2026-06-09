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
    public static final int MAX_BODY_LENGTH = 1_000_000;

    private final UUID id;
    private final UUID organizationId;
    private final UUID senderUserId;
    private final List<UUID> recipientUserIds;
    private final String subject;
    private final String body;
    private final MailType mailType;
    private final MailBodyType bodyType;
    private final RelatedResourceType relatedResourceType;
    private final UUID relatedResourceId;
    private final UUID idempotencyKey;
    private final List<MailAttachment> attachments;
    private MailDeliveryStatus deliveryStatus;
    private Instant requestedAt;
    private Instant sentAt;
    private Instant failedAt;
    private String failureCode;
    private int retryCount;

    private Mail(
            UUID id,
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
            MailDeliveryStatus deliveryStatus,
            Instant requestedAt,
            Instant sentAt,
            Instant failedAt,
            String failureCode,
            int retryCount,
            List<MailAttachment> attachments) {
        this.id = id;
        this.organizationId = requireNonNull(organizationId, "조직 ID는 필수입니다.");
        this.senderUserId = requireNonNull(senderUserId, "발신자 ID는 필수입니다.");
        this.recipientUserIds = new ArrayList<>(validateRecipients(recipientUserIds));
        this.subject = validateSubject(subject);
        this.body = validateBody(body);
        this.mailType = requireNonNull(mailType, "메일 유형은 필수입니다.");
        this.bodyType = requireNonNull(bodyType, "메일 본문 유형은 필수입니다.");
        validateRelatedResource(relatedResourceType, relatedResourceId);
        this.relatedResourceType = relatedResourceType;
        this.relatedResourceId = relatedResourceId;
        this.idempotencyKey = requireNonNull(idempotencyKey, "메일 멱등성 키는 필수입니다.");
        this.deliveryStatus = requireNonNull(deliveryStatus, "메일 발송 상태는 필수입니다.");
        validateDeliveryState(
                deliveryStatus, requestedAt, sentAt, failedAt, failureCode, retryCount);
        this.requestedAt = requestedAt;
        this.sentAt = sentAt;
        this.failedAt = failedAt;
        this.failureCode = failureCode;
        this.retryCount = retryCount;
        this.attachments = new ArrayList<>(requireNonNull(attachments, "첨부파일 목록은 필수입니다."));
        validateAttachments(this.attachments, this.senderUserId);
    }

    public static Mail createDraft(
            UUID organizationId,
            UUID senderUserId,
            List<UUID> recipientUserIds,
            String subject,
            String body,
            MailType mailType,
            MailBodyType bodyType,
            RelatedResourceType relatedResourceType,
            UUID relatedResourceId,
            UUID idempotencyKey) {
        return new Mail(
                null,
                organizationId,
                senderUserId,
                recipientUserIds,
                subject,
                body,
                mailType,
                bodyType,
                relatedResourceType,
                relatedResourceId,
                idempotencyKey,
                MailDeliveryStatus.DRAFT,
                null,
                null,
                null,
                null,
                0,
                List.of());
    }

    public static Mail of(
            UUID id,
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
            MailDeliveryStatus deliveryStatus,
            Instant requestedAt,
            Instant sentAt,
            Instant failedAt,
            String failureCode,
            int retryCount,
            List<MailAttachment> attachments) {
        return new Mail(
                id,
                organizationId,
                senderUserId,
                recipientUserIds,
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
                retryCount,
                attachments);
    }

    public void requestDelivery(Instant requestedAt) {
        if (deliveryStatus != MailDeliveryStatus.DRAFT) {
            throw invalid("작성 중 상태의 메일만 발송 요청할 수 있습니다.");
        }
        this.requestedAt = requireNonNull(requestedAt, "메일 발송 요청 시각은 필수입니다.");
        this.deliveryStatus = MailDeliveryStatus.REQUESTED;
    }

    public void retryDelivery(Instant retriedAt) {
        if (deliveryStatus != MailDeliveryStatus.FAILED) {
            throw invalid("발송 실패 상태의 메일만 재요청할 수 있습니다.");
        }
        Instant validatedRetriedAt = requireNonNull(retriedAt, "메일 재요청 시각은 필수입니다.");
        if (validatedRetriedAt.isBefore(failedAt)) {
            throw invalid("메일 재요청 시각은 발송 실패 시각보다 빠를 수 없습니다.");
        }
        this.deliveryStatus = MailDeliveryStatus.RETRYING;
        this.requestedAt = validatedRetriedAt;
        this.failedAt = null;
        this.failureCode = null;
        this.retryCount++;
    }

    public void markSent(Instant sentAt) {
        ensureDeliveryInProgress();
        Instant validatedSentAt = requireNonNull(sentAt, "메일 발송 완료 시각은 필수입니다.");
        ensureNotBeforeRequested(validatedSentAt, "메일 발송 완료 시각");
        this.deliveryStatus = MailDeliveryStatus.SENT;
        this.sentAt = validatedSentAt;
    }

    public void markFailed(Instant failedAt, String failureCode) {
        ensureDeliveryInProgress();
        Instant validatedFailedAt = requireNonNull(failedAt, "메일 발송 실패 시각은 필수입니다.");
        String validatedFailureCode = requireText(failureCode, "메일 발송 실패 코드는 필수입니다.");
        ensureNotBeforeRequested(validatedFailedAt, "메일 발송 실패 시각");
        this.deliveryStatus = MailDeliveryStatus.FAILED;
        this.failedAt = validatedFailedAt;
        this.failureCode = validatedFailureCode;
    }

    public void addAttachment(MailAttachment attachment) {
        if (deliveryStatus != MailDeliveryStatus.DRAFT) {
            throw invalid("발송 요청 전인 메일에만 첨부파일을 등록할 수 있습니다.");
        }
        MailAttachment validatedAttachment = requireNonNull(attachment, "첨부파일은 필수입니다.");
        boolean duplicated =
                attachments.stream()
                        .anyMatch(
                                existing ->
                                        existing.objectKey()
                                                .equals(validatedAttachment.objectKey()));
        if (duplicated) {
            throw invalid("동일한 object key의 첨부파일을 중복 등록할 수 없습니다.");
        }
        validateAttachmentUploader(validatedAttachment, senderUserId);
        attachments.add(validatedAttachment);
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

    public List<UUID> recipientUserIds() {
        return List.copyOf(recipientUserIds);
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

    public int retryCount() {
        return retryCount;
    }

    public List<MailAttachment> attachments() {
        return List.copyOf(attachments);
    }

    private void ensureDeliveryInProgress() {
        if (deliveryStatus != MailDeliveryStatus.REQUESTED
                && deliveryStatus != MailDeliveryStatus.RETRYING) {
            throw invalid("발송 요청 또는 재요청 상태의 메일만 발송 결과를 변경할 수 있습니다.");
        }
    }

    private void ensureNotBeforeRequested(Instant resultAt, String fieldName) {
        if (resultAt.isBefore(requestedAt)) {
            throw invalid(fieldName + "은 최근 발송 요청 시각보다 빠를 수 없습니다.");
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

    private static String validateBody(String body) {
        String validated = requireText(body, "메일 본문은 필수입니다.");
        if (validated.length() > MAX_BODY_LENGTH) {
            throw invalid("메일 본문은 " + MAX_BODY_LENGTH + "자를 초과할 수 없습니다.");
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
            MailDeliveryStatus status,
            Instant requestedAt,
            Instant sentAt,
            Instant failedAt,
            String failureCode,
            int retryCount) {
        if (retryCount < 0) {
            throw invalid("메일 재시도 횟수는 0보다 작을 수 없습니다.");
        }
        if (status == MailDeliveryStatus.DRAFT) {
            requireState(
                    requestedAt == null
                            && sentAt == null
                            && failedAt == null
                            && failureCode == null
                            && retryCount == 0,
                    "작성 중 상태에는 발송 결과 정보를 지정할 수 없습니다.");
            return;
        }
        requireState(requestedAt != null, "발송 상태의 메일에는 요청 시각이 필요합니다.");
        if (status == MailDeliveryStatus.REQUESTED) {
            requireState(
                    sentAt == null && failedAt == null && failureCode == null && retryCount == 0,
                    "발송 요청 상태의 정보가 올바르지 않습니다.");
        } else if (status == MailDeliveryStatus.RETRYING) {
            requireState(
                    sentAt == null && failedAt == null && failureCode == null && retryCount > 0,
                    "발송 재요청 상태의 정보가 올바르지 않습니다.");
        } else if (status == MailDeliveryStatus.SENT) {
            requireState(
                    sentAt != null && failedAt == null && failureCode == null,
                    "발송 완료 상태의 정보가 올바르지 않습니다.");
            requireState(!sentAt.isBefore(requestedAt), "메일 발송 완료 시각이 요청 시각보다 빠릅니다.");
        } else if (status == MailDeliveryStatus.FAILED) {
            requireState(
                    failedAt != null
                            && failureCode != null
                            && !failureCode.isBlank()
                            && sentAt == null,
                    "발송 실패 상태의 정보가 올바르지 않습니다.");
            requireState(!failedAt.isBefore(requestedAt), "메일 발송 실패 시각이 요청 시각보다 빠릅니다.");
        }
    }

    private static void validateAttachments(List<MailAttachment> attachments, UUID senderUserId) {
        Set<String> objectKeys = new HashSet<>();
        for (MailAttachment attachment : attachments) {
            MailAttachment validated = requireNonNull(attachment, "첨부파일은 null일 수 없습니다.");
            if (!objectKeys.add(validated.objectKey())) {
                throw invalid("동일한 object key의 첨부파일을 중복 등록할 수 없습니다.");
            }
            validateAttachmentUploader(validated, senderUserId);
        }
    }

    private static void validateAttachmentUploader(MailAttachment attachment, UUID senderUserId) {
        if (!senderUserId.equals(attachment.uploaderUserId())) {
            throw invalid("메일 발신자만 첨부파일을 등록할 수 있습니다.");
        }
    }

    private static void requireState(boolean condition, String message) {
        if (!condition) {
            throw invalid(message);
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
