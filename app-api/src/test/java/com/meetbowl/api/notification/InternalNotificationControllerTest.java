package com.meetbowl.api.notification;

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
import com.meetbowl.application.notification.DispatchNotificationCommand;
import com.meetbowl.application.notification.DispatchNotificationUseCase;
import com.meetbowl.application.notification.NotificationResult;

/**
 * 시스템 내부 알림 발송 엔드포인트의 라우팅과 요청 검증을 확인한다.
 *
 * <p>정상 요청이 UseCase 결과(생성된 알림)를 201로 반환하는지, 필수 본문 값이 비면 공통 검증 실패 응답으로 거절하는지를 본다. 권한(@RequireSystem)
 * 자체는 SecurityConfig 통합 검증 영역이라 이 슬라이스에서는 필터를 끄고 컨트롤러 계약만 본다.
 */
@WebMvcTest(controllers = InternalNotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CurrentUserArgumentResolver.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class InternalNotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;
    @MockitoBean private DispatchNotificationUseCase dispatchNotificationUseCase;

    @Test
    @DisplayName("내부 발송 요청을 받아 생성된 알림을 201로 반환한다")
    void send_success() throws Exception {
        UUID notificationId = UUID.randomUUID();
        given(dispatchNotificationUseCase.execute(any(DispatchNotificationCommand.class)))
                .willReturn(
                        new NotificationResult(
                                notificationId,
                                "MINUTES_REVIEW_REQUEST",
                                "회의록 검토 요청",
                                "회의록 검토를 요청합니다.",
                                "MEETING_MINUTES",
                                UUID.randomUUID(),
                                false,
                                null,
                                Instant.parse("2099-01-01T00:00:00Z")));

        mockMvc.perform(
                        post("/api/v1/internal/notifications/send")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "recipientUserId": "33333333-3333-3333-3333-333333333333",
                                          "type": "MINUTES_REVIEW_REQUEST",
                                          "title": "회의록 검토 요청",
                                          "content": "회의록 검토를 요청합니다.",
                                          "resourceType": "MEETING_MINUTES",
                                          "resourceId": "44444444-4444-4444-4444-444444444444"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(notificationId.toString()))
                .andExpect(jsonPath("$.data.type").value("MINUTES_REVIEW_REQUEST"))
                .andExpect(jsonPath("$.data.read").value(false));
    }

    @Test
    @DisplayName("필수 본문 값이 비면 검증 실패로 거절한다")
    void send_failsWhenTitleBlank() throws Exception {
        mockMvc.perform(
                        post("/api/v1/internal/notifications/send")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "recipientUserId": "33333333-3333-3333-3333-333333333333",
                                          "type": "MEETING_REMINDER",
                                          "title": "  ",
                                          "content": "회의가 곧 시작됩니다."
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
}
