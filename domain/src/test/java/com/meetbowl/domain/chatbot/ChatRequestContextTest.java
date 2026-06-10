package com.meetbowl.domain.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class ChatRequestContextTest {

    @Test
    void createsStatelessRequestWithEmptyConversation() {
        ChatAccessContext accessContext =
                new ChatAccessContext(Set.of(ChatSourceType.MINUTES), Set.of());

        ChatRequestContext request = new ChatRequestContext("  배포 일정을 알려줘  ", null, accessContext);

        assertEquals("배포 일정을 알려줘", request.question());
        assertEquals(ChatConversationContext.empty(), request.conversation());
    }

    @Test
    void requiresTrustedAccessContext() {
        assertThrows(
                BusinessException.class,
                () -> new ChatRequestContext("질문", ChatConversationContext.empty(), null));
    }
}
