package com.meetbowl.application.mail;

import java.io.InputStream;

/** 메일 첨부 다운로드 결과다. 원본은 메모리에 적재하지 않고 스트림으로 전달하며, 닫기 책임은 호출 측(컨트롤러)에 있다. */
public record MailAttachmentDownloadResult(
        String originalFileName, String contentType, long sizeBytes, InputStream content) {}
