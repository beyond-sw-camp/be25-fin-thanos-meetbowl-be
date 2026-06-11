package com.meetbowl.api.mail.dto;

import java.util.List;

import com.meetbowl.application.mail.MailPageResult;

/** 메일함 목록 페이지 응답 본문이다. 요약 항목과 페이지 메타를 클라이언트 계약으로 변환해 노출한다. */
public record MailPageResponse(
        List<MailResponse.Summary> items, int page, int size, long totalElements, int totalPages) {

    public static MailPageResponse from(MailPageResult result) {
        return new MailPageResponse(
                result.items().stream().map(MailResponse.Summary::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
