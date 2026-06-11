package com.meetbowl.infrastructure.client.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.chatbot.ChatAnswer;
import com.meetbowl.domain.chatbot.ChatConversationContext;
import com.meetbowl.domain.chatbot.ChatRequestContext;
import com.meetbowl.domain.chatbot.ChatSourceType;

class ChatbotAiClientAdapterTest {

    private static final String BASE_URL = "http://ai-server";
    private static final String CHAT_URL = BASE_URL + "/api/v1/chat";

    private record Fixture(ChatbotAiClientAdapter adapter, MockRestServiceServer server) {}

    private Fixture newFixture() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ChatbotAiClientAdapter adapter = new ChatbotAiClientAdapter(builder.build());
        return new Fixture(adapter, server);
    }

    private ChatRequestContext sampleContext() {
        return new ChatRequestContext(
                "지난 회의 배포 일정 알려줘", ChatConversationContext.empty(), UUID.randomUUID(), Set.of());
    }

    @Test
    @DisplayName("AI 성공 응답을 도메인 ChatAnswer로 검증·변환한다")
    void ask_mapsSuccessfulResponse() {
        Fixture fixture = newFixture();
        UUID resourceId = UUID.randomUUID();
        String responseJson =
                """
                {
                  "success": true,
                  "data": {
                    "answer": "6월 10일까지 1차 배포",
                    "model": "gemini-2.0-flash",
                    "sources": [
                      {
                        "type": "MINUTES",
                        "resourceId": "%s",
                        "title": "배포 일정 회의록",
                        "snippet": "6월 10일까지 1차 배포",
                        "score": 0.9
                      }
                    ]
                  }
                }
                """
                        .formatted(resourceId);
        fixture.server()
                .expect(requestTo(CHAT_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        ChatAnswer answer = fixture.adapter().ask(sampleContext());

        assertEquals("6월 10일까지 1차 배포", answer.answer());
        assertEquals("gemini-2.0-flash", answer.modelName());
        assertEquals("chat-v1", answer.promptVersion());
        assertEquals(1, answer.citations().size());
        assertEquals(ChatSourceType.MINUTES, answer.citations().get(0).sourceType());
        assertEquals(resourceId, answer.citations().get(0).sourceId());
        assertEquals(1, answer.citations().get(0).displayOrder());
        fixture.server().verify();
    }

    @Test
    @DisplayName("출처 식별자가 UUID가 아니면 응답 검증 실패로 변환한다")
    void ask_failsWhenResourceIdInvalid() {
        Fixture fixture = newFixture();
        String responseJson =
                """
                {
                  "success": true,
                  "data": {
                    "answer": "답변",
                    "model": "gemini-2.0-flash",
                    "sources": [
                      {"type": "MINUTES", "resourceId": "not-a-uuid", "title": "t", "snippet": "s"}
                    ]
                  }
                }
                """;
        fixture.server()
                .expect(requestTo(CHAT_URL))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> fixture.adapter().ask(sampleContext()));
        assertEquals(ErrorCode.AI_RESPONSE_VALIDATION_FAILED, exception.errorCode());
    }

    @Test
    @DisplayName("success=false 응답은 검증 실패로 변환한다")
    void ask_failsWhenEnvelopeNotSuccess() {
        Fixture fixture = newFixture();
        fixture.server()
                .expect(requestTo(CHAT_URL))
                .andRespond(
                        withSuccess(
                                "{\"success\": false, \"data\": null}",
                                MediaType.APPLICATION_JSON));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> fixture.adapter().ask(sampleContext()));
        assertEquals(ErrorCode.AI_RESPONSE_VALIDATION_FAILED, exception.errorCode());
    }

    @Test
    @DisplayName("AI 서버 오류는 Provider 사용 불가로 변환한다")
    void ask_failsWhenServerError() {
        Fixture fixture = newFixture();
        fixture.server()
                .expect(requestTo(CHAT_URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> fixture.adapter().ask(sampleContext()));
        assertEquals(ErrorCode.AI_PROVIDER_UNAVAILABLE, exception.errorCode());
    }

    @Test
    @DisplayName("AI 서버 권한 거부는 RAG 접근 거부로 변환한다")
    void ask_failsWhenForbidden() {
        Fixture fixture = newFixture();
        fixture.server().expect(requestTo(CHAT_URL)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> fixture.adapter().ask(sampleContext()));
        assertEquals(ErrorCode.AI_RAG_ACCESS_DENIED, exception.errorCode());
    }
}
