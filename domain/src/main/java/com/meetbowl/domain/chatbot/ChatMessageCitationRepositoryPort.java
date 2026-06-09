package com.meetbowl.domain.chatbot;

import java.util.List;
import java.util.UUID;

public interface ChatMessageCitationRepositoryPort {

    ChatMessageCitation save(ChatMessageCitation citation);

    List<ChatMessageCitation> findByMessageId(UUID messageId);
}
