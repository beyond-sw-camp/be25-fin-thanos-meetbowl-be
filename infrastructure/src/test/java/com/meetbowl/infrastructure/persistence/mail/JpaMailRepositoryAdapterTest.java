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

    @Test
    void searchMatchesSubjectOrBodyAndExcludesTrash() {
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        Mail visible = sentMail(senderId, recipientId, "배포 일정 공유", "다음 배포는 금요일입니다.");
        Mail bodyHit = sentMail(senderId, recipientId, "주간 회의", "배포 관련 안건 포함");
        Mail noHit = sentMail(senderId, recipientId, "점심 메뉴", "오늘은 김치찌개");

        mailboxRepository.saveAll(List.of(MailboxEntry.inbox(visible.id(), recipientId)));
        mailboxRepository.saveAll(List.of(MailboxEntry.inbox(bodyHit.id(), recipientId)));
        mailboxRepository.saveAll(List.of(MailboxEntry.inbox(noHit.id(), recipientId)));

        // 제목 또는 본문 어느 한쪽이라도 키워드를 포함하면 결과에 들어와야 한다.
        assertThat(mailboxRepository.searchPageByOwnerUserId(recipientId, "배포", 0, 20)).hasSize(2);
        assertThat(mailboxRepository.countSearchByOwnerUserId(recipientId, "배포")).isEqualTo(2);

        // 휴지통으로 이동한 항목은 검색에서 제외한다.
        MailboxEntry trashed =
                mailboxRepository
                        .findAccessibleByMailIdAndOwnerUserId(bodyHit.id(), recipientId)
                        .orElseThrow();
        trashed.moveToTrash(Instant.parse("2099-01-01T00:01:00Z"));
        mailboxRepository.save(trashed);

        assertThat(mailboxRepository.searchPageByOwnerUserId(recipientId, "배포", 0, 20)).hasSize(1);
    }

    private Mail sentMail(UUID senderId, UUID recipientId, String subject, String body) {
        Mail draft =
                Mail.createDraft(
                        UUID.randomUUID(),
                        senderId,
                        List.of(recipientId),
                        subject,
                        body,
                        MailType.NORMAL,
                        MailBodyType.TEXT,
                        null,
                        null,
                        UUID.randomUUID());
        Instant now = Instant.parse("2099-01-01T00:00:00Z");
        draft.requestDelivery(now);
        draft.markSent(now);
        return mailRepository.save(draft);
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
