package com.meetbowl.api.mail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
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
import com.meetbowl.application.mail.BackupMailsUseCase;
import com.meetbowl.application.mail.ChangeMailReadStatusUseCase;
import com.meetbowl.application.mail.DownloadMailAttachmentUseCase;
import com.meetbowl.application.mail.GetMailDetailUseCase;
import com.meetbowl.application.mail.ListMailUseCase;
import com.meetbowl.application.mail.MoveMailToTrashUseCase;
import com.meetbowl.application.mail.PermanentlyDeleteMailUseCase;
import com.meetbowl.application.mail.RestoreMailUseCase;
import com.meetbowl.application.mail.SearchMailUseCase;
import com.meetbowl.application.mail.SendAnnouncementUseCase;
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
    @MockitoBean private BackupMailsUseCase backupMailsUseCase;
    @MockitoBean private SearchMailUseCase searchMailUseCase;
    @MockitoBean private SendAnnouncementUseCase sendAnnouncementUseCase;
    @MockitoBean private DownloadMailAttachmentUseCase downloadMailAttachmentUseCase;

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
                        new SendMailResult(mailId, "SENT", Instant.parse("2099-01-01T00:00:00Z")));

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

    @Test
    void searchMail() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        AuthenticatedUser user =
                new AuthenticatedUser(userId, organizationId, AuthenticatedUserRole.USER, "사용자");
        when(searchMailUseCase.execute(eq(userId), eq("배포"), eq(1), eq(20)))
                .thenReturn(
                        new com.meetbowl.application.mail.MailPageResult(List.of(), 1, 20, 0L, 0));

        mockMvc.perform(
                        get("/api/v1/mails/search")
                                .param("q", "배포")
                                .requestAttr(AuthenticatedUserAttributes.CURRENT_USER, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        verify(searchMailUseCase).execute(userId, "배포", 1, 20);
    }

    @Test
    void announce() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID mailId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        AuthenticatedUser admin =
                new AuthenticatedUser(userId, organizationId, AuthenticatedUserRole.ADMIN, "관리자");
        when(sendAnnouncementUseCase.execute(any()))
                .thenReturn(
                        new SendMailResult(mailId, "SENT", Instant.parse("2099-01-01T00:00:00Z")));

        mockMvc.perform(
                        post("/api/v1/mails/announcements")
                                .requestAttr(AuthenticatedUserAttributes.CURRENT_USER, admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "subject": "전사 공지",
                                          "body": "본문",
                                          "bodyType": "TEXT",
                                          "idempotencyKey": "%s"
                                        }
                                        """
                                                .formatted(idempotencyKey)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mailId").value(mailId.toString()))
                .andExpect(jsonPath("$.data.deliveryStatus").value("SENT"));

        verify(sendAnnouncementUseCase)
                .execute(any(com.meetbowl.application.mail.SendAnnouncementCommand.class));
    }
}
