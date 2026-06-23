package com.meetbowl.application.mail;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailAttachment;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.storage.ObjectStoragePort;
import com.meetbowl.domain.storage.StoredObject;

/**
 * 메일 첨부 원본을 다운로드한다.
 *
 * <p>본문(Mail)에 묶인 첨부지만 열람은 발신자 또는 수신자만 허용한다. 권한 검증 후에만 Object Storage를 조회해, 첨부 저장 경로로 우회 접근하지 못하게 한다.
 */
@Service
public class DownloadMailAttachmentUseCase {

    private final MailRepositoryPort mailRepositoryPort;
    private final ObjectStoragePort objectStoragePort;

    public DownloadMailAttachmentUseCase(
            MailRepositoryPort mailRepositoryPort, ObjectStoragePort objectStoragePort) {
        this.mailRepositoryPort = mailRepositoryPort;
        this.objectStoragePort = objectStoragePort;
    }

    @Transactional(readOnly = true)
    public MailAttachmentDownloadResult execute(
            UUID mailId, UUID attachmentId, UUID requesterUserId) {
        Mail mail = MailUseCaseSupport.findMail(mailRepositoryPort, mailId);
        boolean allowed =
                mail.senderUserId().equals(requesterUserId)
                        || mail.recipientUserIds().contains(requesterUserId);
        if (!allowed) {
            throw new BusinessException(
                    ErrorCode.MAIL_FORBIDDEN_ACCESS, "이 메일의 첨부에 접근할 수 없습니다.");
        }
        MailAttachment attachment =
                mail.attachments().stream()
                        .filter(item -> item.id().equals(attachmentId))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "첨부파일을 찾을 수 없습니다."));
        StoredObject object = objectStoragePort.download(attachment.objectKey());
        return new MailAttachmentDownloadResult(
                attachment.originalFileName(),
                attachment.mimeType(),
                attachment.sizeBytes(),
                object.content());
    }
}
