package com.meetbowl.domain.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class ChatSessionTest {

    @Test
    void startGeneralChatSession() {
        UUID ownerUserId = UUID.randomUUID();
        Instant now = Instant.parse("2099-01-01T01:00:00Z");

        ChatSession session =
                ChatSession.start(ownerUserId, "회의록 검색", ChatScopeType.GENERAL, null, now);

        assertEquals(ownerUserId, session.ownerUserId());
        assertEquals("회의록 검색", session.title());
        assertEquals(ChatSessionStatus.ACTIVE, session.status());
        assertFalse(session.isDeleted());
    }

    @Test
    void scopedChatSessionRequiresScopeId() {
        UUID ownerUserId = UUID.randomUUID();
        Instant now = Instant.parse("2099-01-01T01:00:00Z");

        assertThrows(
                BusinessException.class,
                () -> ChatSession.start(ownerUserId, "회의 질문", ChatScopeType.MEETING, null, now));
    }

    @Test
    void deleteChatSession() {
        UUID ownerUserId = UUID.randomUUID();
        Instant now = Instant.parse("2099-01-01T01:00:00Z");
        ChatSession session =
                ChatSession.start(ownerUserId, "회의록 검색", ChatScopeType.GENERAL, null, now);

        ChatSession deleted = session.delete(Instant.parse("2099-01-02T01:00:00Z"));

        assertTrue(deleted.isDeleted());
        assertEquals(ChatSessionStatus.DELETED, deleted.status());
    }
}
