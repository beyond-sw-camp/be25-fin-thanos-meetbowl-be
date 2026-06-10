package com.meetbowl.infrastructure.persistence.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailBodyType;
import com.meetbowl.domain.mail.MailType;
import com.meetbowl.domain.mail.MailboxEntry;
import com.meetbowl.domain.mail.MailboxType;
import com.meetbowl.infrastructure.config.InfrastructureConfig;

@SpringBootTest(classes = JpaMailRepositoryAdapterTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:mail-api-jpa-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class JpaMailRepositoryAdapterTest {

    @Autowired private JpaMailRepositoryAdapter mailRepository;
    @Autowired private JpaMailboxEntryRepositoryAdapter mailboxRepository;

    @Test
    void savesAndFiltersMailboxEntries() {
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        Mail draft =
                Mail.createDraft(
                        UUID.randomUUID(),
                        senderId,
                        List.of(recipientId),
                        "제목",
                        "본문",
                        MailType.NORMAL,
                        MailBodyType.TEXT,
                        null,
                        null,
                        UUID.randomUUID());
        draft.requestDelivery(Instant.parse("2099-01-01T00:00:00Z"));
        Mail saved = mailRepository.save(draft);
        mailboxRepository.saveAll(
                List.of(
                        MailboxEntry.sent(saved.id(), senderId),
                        MailboxEntry.inbox(saved.id(), recipientId)));

        List<MailboxEntry> inbox =
                mailboxRepository.findPageByOwnerUserIdAndMailboxType(
                        recipientId, MailboxType.INBOX, 0, 20);

        assertThat(inbox).hasSize(1);
        assertThat(inbox.getFirst().mailId()).isEqualTo(saved.id());
        assertThat(mailRepository.findByIdempotencyKey(saved.idempotencyKey())).isPresent();

        MailboxEntry trashed = inbox.getFirst();
        trashed.moveToTrash(Instant.parse("2099-01-01T00:01:00Z"));
        mailboxRepository.save(trashed);

        assertThat(
                        mailboxRepository.findPageByOwnerUserIdAndMailboxType(
                                recipientId, MailboxType.INBOX, 0, 20))
                .isEmpty();
        assertThat(mailboxRepository.findTrashPageByOwnerUserId(recipientId, 0, 20)).hasSize(1);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        JpaMailRepositoryAdapter.class,
        JpaMailboxEntryRepositoryAdapter.class,
        MailJpaConfig.class
    })
    static class TestApplication {}
}
