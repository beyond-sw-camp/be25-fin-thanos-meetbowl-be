package com.meetbowl.api.mail.dto;

import java.util.List;

import com.meetbowl.application.mail.MailPageResult;

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
