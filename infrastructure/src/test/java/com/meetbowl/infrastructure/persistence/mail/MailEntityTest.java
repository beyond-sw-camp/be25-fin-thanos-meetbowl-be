package com.meetbowl.infrastructure.persistence.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.mail.Mail;
import com.meetbowl.domain.mail.MailAttachment;
import com.meetbowl.domain.mail.MailBodyType;
import com.meetbowl.domain.mail.MailDeliveryStatus;
import com.meetbowl.domain.mail.MailType;
import com.meetbowl.infrastructure.config.InfrastructureConfig;

@Transactional
@SpringBootTest(classes = MailEntityTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:mail-entity-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class MailEntityTest {

    @PersistenceContext private EntityManager entityManager;

    @Test
    void persistAndRestoreMailAggregate() {
        UUID recipientUserId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        Mail mail =
                Mail.request(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        List.of(recipientUserId),
                        "회의록 공유",
                        "회의록을 공유합니다.",
                        MailType.NORMAL,
                        MailBodyType.TEXT,
                        null,
                        null,
                        idempotencyKey,
                        Instant.parse("2026-06-08T01:00:00Z"));
        mail.addAttachment(
                MailAttachment.create(
                        mail.senderUserId(),
                        "attachments/mail/file.pdf",
                        "회의록.pdf",
                        "file.pdf",
                        "application/pdf",
                        1024));
        mail.markSent(Instant.parse("2026-06-08T01:01:00Z"));

        MailEntity entity = MailEntity.from(mail);
        entityManager.persist(entity);
        entityManager.flush();
        entityManager.clear();

        Mail restored = entityManager.find(MailEntity.class, entity.getId()).toDomain();

        assertThat(restored.id()).isNotNull();
        assertThat(restored.idempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(restored.deliveryStatus()).isEqualTo(MailDeliveryStatus.SENT);
        assertThat(restored.recipientUserIds()).containsExactly(recipientUserId);
        assertThat(restored.mailboxEntries()).hasSize(2);
        assertThat(restored.attachments()).hasSize(1);
        assertThat(restored.attachments().getFirst().objectKey())
                .isEqualTo("attachments/mail/file.pdf");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(InfrastructureConfig.class)
    static class TestApplication {}
}
