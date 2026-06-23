package com.meetbowl.application.admin;

import java.util.List;

public record AdminAuditLogPageResult(
        List<AdminAuditLogResult> items, int page, int size, long totalElements, int totalPages) {}
