package com.meetbowl.application.mail;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;

/** 현재 사용자가 소유한 메일함 항목을 제목/본문 키워드로 검색한다. 휴지통/영구 삭제 항목은 제외한다. */
@Service
public class SearchMailUseCase {

    private final MailRepositoryPort mailRepositoryPort;
    private final MailboxEntryRepositoryPort mailboxEntryRepositoryPort;

    public SearchMailUseCase(
            MailRepositoryPort mailRepositoryPort,
            MailboxEntryRepositoryPort mailboxEntryRepositoryPort) {
        this.mailRepositoryPort = mailRepositoryPort;
        this.mailboxEntryRepositoryPort = mailboxEntryRepositoryPort;
    }

    @Transactional(readOnly = true)
    public MailPageResult execute(UUID ownerUserId, String keyword, int page, int size) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "검색어를 입력해 주세요.");
        }

        int offset = (page - 1) * size;
        List<MailboxEntry> entries =
                mailboxEntryRepositoryPort.searchPageByOwnerUserId(
                        ownerUserId, normalized, offset, size);
        long total = mailboxEntryRepositoryPort.countSearchByOwnerUserId(ownerUserId, normalized);

        List<MailSummaryResult> items =
                entries.stream()
                        .map(
                                entry ->
                                        MailUseCaseSupport.summary(
                                                MailUseCaseSupport.findMail(
                                                        mailRepositoryPort, entry.mailId()),
                                                entry))
                        .toList();
        int totalPages = (int) Math.ceil((double) total / size);
        return new MailPageResult(items, page, size, total, totalPages);
    }
}
