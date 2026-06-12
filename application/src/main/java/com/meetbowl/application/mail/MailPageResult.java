package com.meetbowl.application.mail;

import java.util.List;

/** 메일함 목록의 한 페이지다. 항목 요약과 페이지 메타(현재 페이지·크기·전체 개수·전체 페이지 수)를 담는다. */
public record MailPageResult(
        List<MailSummaryResult> items, int page, int size, long totalElements, int totalPages) {}
