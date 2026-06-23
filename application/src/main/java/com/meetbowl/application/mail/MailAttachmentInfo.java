package com.meetbowl.application.mail;

import java.util.UUID;

/** 메일 상세 응답에 노출하는 첨부 메타데이터다. 원본 바이트는 포함하지 않고 다운로드는 별도 API로 받는다. */
public record MailAttachmentInfo(
        UUID attachmentId, String originalFileName, String mimeType, long sizeBytes) {}
