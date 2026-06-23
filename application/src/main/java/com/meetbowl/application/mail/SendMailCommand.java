package com.meetbowl.application.mail;

import java.util.List;
import java.util.UUID;

/** 사용자 메일 발송 UseCase의 입력 계약이다. 발신자/조직 ID는 인증 사용자에서 채우고 본문으로 받지 않는다. */
public record SendMailCommand(
        UUID organizationId,
        UUID senderUserId,
        List<UUID> recipientUserIds,
        String subject,
        String body,
        String bodyType,
        String relatedResourceType,
        UUID relatedResourceId,
        UUID idempotencyKey,
        List<AttachmentUpload> attachments) {

    public SendMailCommand {
        // 첨부는 선택이므로 null이면 빈 목록으로 정규화해 호출부 분기를 없앤다.
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    /** 첨부 없는 기존 호출 호환용 생성자. */
    public SendMailCommand(
            UUID organizationId,
            UUID senderUserId,
            List<UUID> recipientUserIds,
            String subject,
            String body,
            String bodyType,
            String relatedResourceType,
            UUID relatedResourceId,
            UUID idempotencyKey) {
        this(
                organizationId,
                senderUserId,
                recipientUserIds,
                subject,
                body,
                bodyType,
                relatedResourceType,
                relatedResourceId,
                idempotencyKey,
                List.of());
    }

    /** 전송 요청과 함께 올라온 첨부 파일 원본과 메타데이터다. */
    public record AttachmentUpload(String originalFileName, String contentType, byte[] content) {}
}
