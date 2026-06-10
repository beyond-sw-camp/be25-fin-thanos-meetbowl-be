package com.meetbowl.domain.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class ChatCitationTest {

    @Test
    void keepsTheActualSharedWorkspaceFileVersionId() {
        UUID fileVersionId = UUID.randomUUID();

        ChatCitation citation =
                new ChatCitation(
                        ChatSourceType.SHARED_WORKSPACE_FILE_VERSION,
                        fileVersionId,
                        "공유 계획서 v3",
                        "배포 일정은 6월 10일입니다.",
                        "/shared-workspaces/files/versions/" + fileVersionId,
                        0.91D,
                        1);

        assertEquals(fileVersionId, citation.sourceId());
    }

    @Test
    void rejectsScoreOutsideNormalizedRange() {
        assertThrows(
                BusinessException.class,
                () ->
                        new ChatCitation(
                                ChatSourceType.MINUTES,
                                UUID.randomUUID(),
                                "회의록",
                                "근거",
                                null,
                                1.5D,
                                1));
    }
}
