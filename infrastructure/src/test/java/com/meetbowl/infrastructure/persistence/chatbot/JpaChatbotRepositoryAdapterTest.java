package com.meetbowl.infrastructure.persistence.chatbot;

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

import com.meetbowl.domain.chatbot.ChatMessage;
import com.meetbowl.domain.chatbot.ChatMessageCitation;
import com.meetbowl.domain.chatbot.ChatScopeType;
import com.meetbowl.domain.chatbot.ChatSession;
import com.meetbowl.domain.chatbot.ChatSourceType;
import com.meetbowl.infrastructure.config.InfrastructureConfig;

@SpringBootTest(classes = JpaChatbotRepositoryAdapterTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:chatbot-jpa-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class JpaChatbotRepositoryAdapterTest {

    @Autowired private JpaChatSessionRepositoryAdapter sessionAdapter;
    @Autowired private JpaChatMessageRepositoryAdapter messageAdapter;
    @Autowired private JpaChatMessageCitationRepositoryAdapter citationAdapter;

    @Test
    void saveAndFindChatSessionMessagesAndCitations() {
        UUID ownerUserId = UUID.randomUUID();
        Instant now = Instant.parse("2099-01-01T01:00:00Z");
        ChatSession session =
                ChatSession.start(ownerUserId, "회의록 질문", ChatScopeType.GENERAL, null, now);

        ChatSession savedSession = sessionAdapter.save(session);
        ChatMessage userMessage =
                ChatMessage.user(savedSession.id(), ownerUserId, 1, "지난 결정사항 알려줘", now);
        ChatMessage assistantMessage =
                ChatMessage.assistant(
                        savedSession.id(),
                        2,
                        "지난 회의 결정사항입니다.",
                        "gemini-2.5",
                        "chat-v1",
                        "ai-req-1",
                        now);

        ChatMessage savedUserMessage = messageAdapter.save(userMessage);
        ChatMessage savedAssistantMessage = messageAdapter.save(assistantMessage);
        ChatMessageCitation citation =
                ChatMessageCitation.create(
                        savedAssistantMessage.id(),
                        ChatSourceType.MINUTES,
                        UUID.randomUUID(),
                        "주간 회의록",
                        "결정사항 근거",
                        null,
                        0.9D,
                        1,
                        now);

        ChatMessageCitation savedCitation = citationAdapter.save(citation);

        List<ChatSession> sessions = sessionAdapter.findActiveByOwnerUserId(ownerUserId);
        List<ChatMessage> messages = messageAdapter.findBySessionId(savedSession.id());
        List<ChatMessageCitation> citations =
                citationAdapter.findByMessageId(savedAssistantMessage.id());

        assertThat(savedSession.id()).isNotNull();
        assertThat(savedUserMessage.id()).isNotNull();
        assertThat(savedCitation.id()).isNotNull();
        assertThat(sessions).hasSize(1);
        assertThat(messages).extracting(ChatMessage::sequenceNumber).containsExactly(1, 2);
        assertThat(citations).hasSize(1);
        assertThat(citations.getFirst().title()).isEqualTo("주간 회의록");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        ChatbotJpaConfig.class,
        JpaChatSessionRepositoryAdapter.class,
        JpaChatMessageRepositoryAdapter.class,
        JpaChatMessageCitationRepositoryAdapter.class
    })
    static class TestApplication {}
}
