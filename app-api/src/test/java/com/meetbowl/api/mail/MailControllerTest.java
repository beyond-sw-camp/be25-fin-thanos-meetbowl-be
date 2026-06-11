package com.meetbowl.api.mail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.meetbowl.api.common.GlobalExceptionHandler;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.AuthenticatedUserAttributes;
import com.meetbowl.api.common.auth.AuthenticatedUserRole;
import com.meetbowl.api.common.auth.CurrentUserArgumentResolver;
import com.meetbowl.api.config.WebMvcConfig;
import com.meetbowl.application.auth.AccessTokenValidationService;
import com.meetbowl.application.mail.ChangeMailReadStatusUseCase;
import com.meetbowl.application.mail.GetMailDetailUseCase;
import com.meetbowl.application.mail.ListMailUseCase;
import com.meetbowl.application.mail.MoveMailToTrashUseCase;
import com.meetbowl.application.mail.PermanentlyDeleteMailUseCase;
import com.meetbowl.application.mail.RestoreMailUseCase;
import com.meetbowl.application.mail.SendMailCommand;
import com.meetbowl.application.mail.SendMailResult;
import com.meetbowl.application.mail.SendMailUseCase;

@WebMvcTest(controllers = MailController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CurrentUserArgumentResolver.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class MailControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;
    @MockitoBean private SendMailUseCase sendMailUseCase;
    @MockitoBean private ListMailUseCase listMailUseCase;
    @MockitoBean private GetMailDetailUseCase getMailDetailUseCase;
    @MockitoBean private ChangeMailReadStatusUseCase changeMailReadStatusUseCase;
    @MockitoBean private MoveMailToTrashUseCase moveMailToTrashUseCase;
    @MockitoBean private RestoreMailUseCase restoreMailUseCase;
    @MockitoBean private PermanentlyDeleteMailUseCase permanentlyDeleteMailUseCase;

    @Test
    void sendMail() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID mailId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        AuthenticatedUser user =
                new AuthenticatedUser(userId, organizationId, AuthenticatedUserRole.USER, "사용자");
        when(sendMailUseCase.execute(any()))
                .thenReturn(
                        new SendMailResult(
                                mailId, "SENT", Instant.parse("2099-01-01T00:00:00Z")));

        mockMvc.perform(
                        post("/api/v1/mails")
                                .requestAttr(AuthenticatedUserAttributes.CURRENT_USER, user)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "recipientUserIds": ["%s"],
                                          "subject": "제목",
                                          "body": "본문",
                                          "bodyType": "TEXT",
                                          "idempotencyKey": "%s"
                                        }
                                        """
                                                .formatted(recipientId, idempotencyKey)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mailId").value(mailId.toString()))
                .andExpect(jsonPath("$.data.deliveryStatus").value("SENT"));

        verify(sendMailUseCase).execute(any(SendMailCommand.class));
    }
}
