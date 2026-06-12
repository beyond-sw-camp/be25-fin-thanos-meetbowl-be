package com.meetbowl.api.chatbot;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.meetbowl.application.chatbot.AskChatbotCommand;

/**
 * 챗봇 질의 API 요청 DTO다.
 *
 * <p>userId와 권한 범위는 본문으로 받지 않는다. 사용자가 권한을 임의로 지정하지 못하도록 인증 결과에서만 채우기 위함이다. messageHistory는 현재 화면이
 * 보유한 휘발성 대화 문맥이며, 상세 검증(역할 교차, 시작/종료 규칙)은 도메인 계층이 수행한다.
 */
public record ChatMessageRequest(
        @NotBlank(message = "챗봇 질문은 필수입니다.") String question,
        @Valid @Size(max = 20, message = "챗봇 대화 이력은 최대 20개 메시지까지 전달할 수 있습니다.")
                List<Message> messageHistory) {

    /** 대화 이력 한 개 메시지다. 신뢰할 수 없는 system 등 임의 역할을 경계에서 먼저 차단한다. */
    public record Message(
            @NotBlank(message = "챗봇 메시지 역할은 필수입니다.")
                    @Pattern(
                            regexp = "(?i)(user|assistant)",
                            message = "챗봇 메시지 역할은 user 또는 assistant만 허용합니다.")
                    String role,
            @NotBlank(message = "챗봇 메시지 내용은 필수입니다.") String content) {}

    public AskChatbotCommand toCommand(UUID userId, UUID organizationId) {
        List<AskChatbotCommand.Message> messages =
                messageHistory == null
                        ? List.of()
                        : messageHistory.stream()
                                .map(
                                        message ->
                                                new AskChatbotCommand.Message(
                                                        message.role(), message.content()))
                                .toList();
        return new AskChatbotCommand(userId, organizationId, question, messages);
    }
}
