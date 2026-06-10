package com.meetbowl.domain.chatbot;

import java.util.List;
import java.util.UUID;

/** AI 답변 출처 저장과 메시지별 출처 조회를 위한 도메인 Port다. */
public interface ChatMessageCitationRepositoryPort {

    ChatMessageCitation save(ChatMessageCitation citation);

    List<ChatMessageCitation> findByMessageId(UUID messageId);
}
