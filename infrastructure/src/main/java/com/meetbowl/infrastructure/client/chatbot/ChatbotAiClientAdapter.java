package com.meetbowl.infrastructure.client.chatbot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

/**
 * 챗봇 답변 생성을 meetbowl-ai 내부 REST API에 위임하는 adapter다.
 *
 * <p>외부 응답을 그대로 신뢰하지 않고 도메인 {@link ChatAnswer} 스키마로 다시 검증한다. 검증 실패는 AI_RESPONSE_VALIDATION_FAILED로,
 * 연결 불가나 서버 오류는 AI_PROVIDER_UNAVAILABLE로, 권한 거부는 AI_RAG_ACCESS_DENIED로 변환해 Provider 상세를 외부로 노출하지
 * 않는다.
 *
 * <p>질문과 답변 본문은 저장하지 않으며 로그에도 남기지 않는다.
 */
@Component
public class ChatbotAiClientAdapter implements ChatbotAiPort {

    private static final String CHAT_URI = "/api/v1/chat";

    // AI 응답에 promptVersion이 없을 때 도메인 ChatAnswer의 필수 필드를 채우는 기본값.
    // 클라이언트 응답으로 노출하지 않는 내부 값이라 설정으로 분리하지 않고 상수로 둔다.
    private static final String FALLBACK_PROMPT_VERSION = "chat-v1";

    private final RestClient aiServerRestClient;

    public ChatbotAiClientAdapter(RestClient aiServerRestClient) {
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
            // AI 서버가 권한 거부를 내려준 경우는 사용자 권한 문제이므로 별도 코드로 구분한다.
            HttpStatusCode status = exception.getStatusCode();
            if (status.value() == 403) {
                throw new BusinessException(ErrorCode.AI_RAG_ACCESS_DENIED);
            }
            throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
        } catch (RestClientException exception) {
            // 연결 실패, 타임아웃, 본문 변환 실패 등은 외부 의존성 장애로 통일한다.
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
            // 도메인 검증 실패와 식별자 파싱 실패를 모두 AI 응답 검증 실패로 묶어 잘못된 답변이 외부 계약으로 굳지 않게 한다.
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
            // 표시 순서는 AI 응답의 정렬을 그대로 따르며 1부터 부여한다.
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
