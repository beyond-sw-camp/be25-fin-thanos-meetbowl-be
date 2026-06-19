package com.meetbowl.infrastructure.client.chatbot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.chatbot.ChatAnswer;
import com.meetbowl.domain.chatbot.ChatCitation;
import com.meetbowl.domain.chatbot.ChatRequestContext;
import com.meetbowl.domain.chatbot.ChatSourceType;
import com.meetbowl.domain.chatbot.ChatbotAiPort;
import com.meetbowl.infrastructure.client.chatbot.dto.AiChatRequest;
import com.meetbowl.infrastructure.client.chatbot.dto.AiChatResponse;

/** Adapter that delegates chatbot requests to the internal meetbowl-ai REST API. */
@Component
public class ChatbotAiClientAdapter implements ChatbotAiPort {

    private static final String CHAT_URI = "/api/v1/chat";
    private static final String FALLBACK_PROMPT_VERSION = "chat-v1";

    private final RestClient aiServerRestClient;

    public ChatbotAiClientAdapter(@Qualifier("aiServerRestClient") RestClient aiServerRestClient) {
        this.aiServerRestClient = aiServerRestClient;
    }

    @Override
    public ChatAnswer ask(ChatRequestContext requestContext) {
        AiChatRequest body =
                AiChatRequest.from(requestContext, UUID.randomUUID(), UUID.randomUUID());

        AiChatResponse response = callAiServer(body);
        if (response == null || !response.success() || response.data() == null) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_VALIDATION_FAILED);
        }
        return toDomain(response.data());
    }

    private AiChatResponse callAiServer(AiChatRequest body) {
        try {
            return aiServerRestClient
                    .post()
                    .uri(CHAT_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(AiChatResponse.class);
        } catch (RestClientResponseException exception) {
            HttpStatusCode status = exception.getStatusCode();
            if (status.value() == 403) {
                throw new BusinessException(ErrorCode.AI_RAG_ACCESS_DENIED);
            }
            throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
        }
    }

    private ChatAnswer toDomain(AiChatResponse.Data data) {
        try {
            List<ChatCitation> citations = toCitations(data.sources());
            String promptVersion =
                    (data.promptVersion() == null || data.promptVersion().isBlank())
                            ? FALLBACK_PROMPT_VERSION
                            : data.promptVersion();
            return new ChatAnswer(data.answer(), citations, data.model(), promptVersion);
        } catch (BusinessException | IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_VALIDATION_FAILED);
        }
    }

    private List<ChatCitation> toCitations(List<AiChatResponse.Source> sources) {
        if (sources == null) {
            return List.of();
        }
        List<ChatCitation> citations = new ArrayList<>(sources.size());
        int displayOrder = 1;
        for (AiChatResponse.Source source : sources) {
            citations.add(
                    new ChatCitation(
                            ChatSourceType.valueOf(source.type()),
                            UUID.fromString(source.resourceId()),
                            source.title(),
                            source.snippet(),
                            null,
                            source.score(),
                            displayOrder++));
        }
        return citations;
    }
}
