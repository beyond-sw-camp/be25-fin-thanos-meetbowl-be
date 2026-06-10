package com.meetbowl.domain.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class ChatMessageCitationTest {

    @Test
    void sourceTypesMatchSearchableResources() {
        assertEquals(5, ChatSourceType.values().length);
        assertTrue(
                java.util.Set.of(ChatSourceType.values())
                        .containsAll(
                                java.util.Set.of(
                                        ChatSourceType.BACKUP_MAIL,
                                        ChatSourceType.MINUTES,
                                        ChatSourceType.PERSONAL_MEMO,
                                        ChatSourceType.PERSONAL_DRIVE_FILE,
                                        ChatSourceType.SHARED_WORKSPACE_FILE_VERSION)));
    }

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

    @Test
    void sharedWorkspaceCitationStoresFileVersionIdAsSourceId() {
        UUID fileVersionId = UUID.randomUUID();

        ChatMessageCitation citation =
                ChatMessageCitation.create(
                        UUID.randomUUID(),
                        ChatSourceType.SHARED_WORKSPACE_FILE_VERSION,
                        fileVersionId,
                        "공유 프로젝트 계획서 v3",
                        "배포 일정은 6월 10일입니다.",
                        "/shared-workspaces/files/versions/" + fileVersionId,
                        0.91D,
                        1,
                        Instant.parse("2099-01-01T01:00:00Z"));

        assertEquals(fileVersionId, citation.sourceId());
    }
}
