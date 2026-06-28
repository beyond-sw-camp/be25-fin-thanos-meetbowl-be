package com.meetbowl.application.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailBodyType;
import com.meetbowl.domain.mail.MailDeliveryStatus;
import com.meetbowl.domain.mail.MailRepositoryPort;
import com.meetbowl.domain.mail.MailType;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxEntryRepositoryPort;
import com.meetbowl.domain.mail.MailboxType;

class SearchMailUseCaseTest {

    private final MailRepositoryPort mailRepository = mock(MailRepositoryPort.class);
    private final MailboxEntryRepositoryPort mailboxRepository =
            mock(MailboxEntryRepositoryPort.class);
    private final SearchMailUseCase useCase =
            new SearchMailUseCase(mailRepository, mailboxRepository);

    @Test
    void searchReturnsMatchedMailboxEntriesAsSummaries() {
        UUID ownerId = UUID.randomUUID();
        UUID mailId = UUID.randomUUID();
        MailboxEntry entry = MailboxEntry.inbox(mailId, ownerId);
        Mail mail =
                Mail.of(
                        mailId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        List.of(ownerId),
                        List.of(),
                        "배포 일정 공유",
                        "다음 배포는 금요일입니다.",
                        MailType.NORMAL,
                        MailBodyType.TEXT,
                        null,
                        null,
                        UUID.randomUUID(),
                        MailDeliveryStatus.SENT,
                        java.time.Instant.parse("2026-06-01T00:00:00Z"),
                        java.time.Instant.parse("2026-06-01T00:00:00Z"),
                        null,
                        null,
                        0,
                        List.of());

        when(mailboxRepository.searchPageByOwnerUserId(ownerId, "배포", 0, 20))
                .thenReturn(List.of(entry));
        when(mailboxRepository.countSearchByOwnerUserId(ownerId, "배포")).thenReturn(1L);
        when(mailRepository.findById(mailId)).thenReturn(java.util.Optional.of(mail));

        MailPageResult result = useCase.execute(ownerId, "배포", 1, 20);

        assertEquals(1, result.items().size());
        assertEquals(mailId, result.items().get(0).mailId());
        assertEquals(MailboxType.INBOX.name(), result.items().get(0).mailboxType());
        assertEquals(1L, result.totalElements());
        assertEquals(1, result.totalPages());
    }

    @Test
    void blankKeywordIsRejected() {
        assertThrows(
                BusinessException.class, () -> useCase.execute(UUID.randomUUID(), "   ", 1, 20));
    }
}
