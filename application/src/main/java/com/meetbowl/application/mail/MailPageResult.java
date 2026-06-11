package com.meetbowl.application.mail;

import java.util.List;

public record MailPageResult(
        List<MailSummaryResult> items, int page, int size, long totalElements, int totalPages) {}
