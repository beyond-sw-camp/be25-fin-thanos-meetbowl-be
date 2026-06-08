package com.meetbowl.domain.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class ChatMessageCitationTest {

    @Test
    void createCitation() {
        UUID messageId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        Instant now = Instant.parse("2099-01-01T01:00:00Z");

        ChatMessageCitation citation =
                ChatMessageCitation.create(
                        messageId,
                        ChatSourceType.MINUTES,
                        sourceId,
                        "주간 회의록",
                        "후속 조치는 금요일까지 완료하기로 했습니다.",
                        "/minutes/" + sourceId,
                        0.87D,
                        1,
                        now);

        assertEquals(ChatSourceType.MINUTES, citation.sourceType());
        assertEquals(1, citation.displayOrder());
    }

    @Test
    void citationScoreMustBeBetweenZeroAndOne() {
        assertThrows(
                BusinessException.class,
                () ->
                        ChatMessageCitation.create(
                                UUID.randomUUID(),
                                ChatSourceType.MINUTES,
                                UUID.randomUUID(),
                                "회의록",
                                "근거",
                                null,
                                1.5D,
                                1,
                                Instant.parse("2099-01-01T01:00:00Z")));
    }
}
