package com.meetbowl.api.chatbot;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.RequireUserOrAdmin;
import com.meetbowl.application.chatbot.AskChatbotUseCase;
import com.meetbowl.application.chatbot.ChatAnswerResult;
import com.meetbowl.common.response.ApiResponse;

/**
 * AI 챗봇 Gateway Controller다.
 *
 * <p>프론트엔드는 meetbowl-ai를 직접 호출하지 않고 이 API만 호출한다. 여기서는 인증된 사용자만 통과시키고, 권한 계산과 AI 위임은 UseCase가 담당한다.
 * Guest는 USER/ADMIN 권한이 아니므로 접근할 수 없다.
 *
 * <p>대화는 영속 데이터가 아니므로 세션 목록, 대화 상세, 복구, 삭제 API를 두지 않고 단일 질의 엔드포인트만 제공한다.
 */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/ai/chat")
public class ChatMessagesController extends BaseController {

    private final AskChatbotUseCase askChatbotUseCase;

    public ChatMessagesController(AskChatbotUseCase askChatbotUseCase) {
        this.askChatbotUseCase = askChatbotUseCase;
    }

    @PostMapping("/messages")
    @RequireUserOrAdmin
    public ApiResponse<ChatMessageResponse> sendMessage(
            @CurrentUser AuthenticatedUser user, @Valid @RequestBody ChatMessageRequest request) {
        ChatAnswerResult result =
                askChatbotUseCase.execute(request.toCommand(user.userId(), user.organizationId()));
        return ok(ChatMessageResponse.from(result));
    }
}
