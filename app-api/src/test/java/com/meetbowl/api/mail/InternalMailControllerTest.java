package com.meetbowl.api.mail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.meetbowl.api.common.GlobalExceptionHandler;
import com.meetbowl.api.common.auth.CurrentUserArgumentResolver;
import com.meetbowl.api.config.WebMvcConfig;
import com.meetbowl.application.auth.AccessTokenValidationService;
import com.meetbowl.application.mail.DispatchInternalMailCommand;
import com.meetbowl.application.mail.DispatchInternalMailUseCase;
import com.meetbowl.application.mail.SendMailResult;

@WebMvcTest(controllers = InternalMailController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CurrentUserArgumentResolver.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class InternalMailControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;
    @MockitoBean private DispatchInternalMailUseCase dispatchInternalMailUseCase;

    @Test
    @DisplayName("내부 발송 요청을 받아 메일 ID와 발송 상태를 반환한다")
    void send_success() throws Exception {
        UUID mailId = UUID.randomUUID();
        given(dispatchInternalMailUseCase.execute(any(DispatchInternalMailCommand.class)))
                .willReturn(
                        new SendMailResult(mailId, "SENT", Instant.parse("2099-01-01T00:00:00Z")));

        mockMvc.perform(
                        post("/api/v1/internal/mails/send")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "organizationId": "11111111-1111-1111-1111-111111111111",
                                          "senderUserId": "22222222-2222-2222-2222-222222222222",
                                          "recipientUserIds": ["33333333-3333-3333-3333-333333333333"],
                                          "subject": "회의록 공유",
                                          "body": "회의록을 공유합니다.",
                                          "bodyType": "MINUTES_SHARE",
                                          "relatedResourceType": "MEETING_MINUTES",
                                          "relatedResourceId": "44444444-4444-4444-4444-444444444444",
                                          "idempotencyKey": "55555555-5555-5555-5555-555555555555"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mailId").value(mailId.toString()))
                .andExpect(jsonPath("$.data.deliveryStatus").value("SENT"));
    }

    @Test
    @DisplayName("필수 본문 값이 비면 검증 실패로 거절한다")
    void send_failsWhenBodyBlank() throws Exception {
        mockMvc.perform(
                        post("/api/v1/internal/mails/send")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "organizationId": "11111111-1111-1111-1111-111111111111",
                                          "senderUserId": "22222222-2222-2222-2222-222222222222",
                                          "recipientUserIds": ["33333333-3333-3333-3333-333333333333"],
                                          "subject": "회의록 공유",
                                          "body": "  ",
                                          "bodyType": "MINUTES_SHARE",
                                          "idempotencyKey": "55555555-5555-5555-5555-555555555555"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
}
