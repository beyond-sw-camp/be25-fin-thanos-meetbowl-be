package com.meetbowl.domain.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class ChatAccessContextTest {

    @Test
    void copiesPermissionSetsBecauseAuthorizationMustNotChangeDuringAiExecution() {
        Set<ChatSourceType> sourceTypes = new HashSet<>(Set.of(ChatSourceType.MINUTES));
        Set<UUID> workspaceIds = new HashSet<>();

        ChatAccessContext context = new ChatAccessContext(sourceTypes, workspaceIds);
        sourceTypes.add(ChatSourceType.BACKUP_MAIL);

        assertEquals(Set.of(ChatSourceType.MINUTES), context.allowedSourceTypes());
        assertThrows(
                UnsupportedOperationException.class,
                () -> context.allowedSourceTypes().add(ChatSourceType.PERSONAL_MEMO));
    }

    @Test
    void rejectsEmptySourceTypesInsteadOfFallingBackToUnfilteredSearch() {
        assertThrows(BusinessException.class, () -> new ChatAccessContext(Set.of(), Set.of()));
    }
}
