package com.meetbowl.api.mail;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiHeaders;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.RequireAdmin;
import com.meetbowl.api.common.auth.RequireUserOrAdmin;
import com.meetbowl.api.mail.dto.BackupMailResponse;
import com.meetbowl.api.mail.dto.BackupMailsRequest;
import com.meetbowl.api.mail.dto.ChangeMailReadRequest;
import com.meetbowl.api.mail.dto.MailPageResponse;
import com.meetbowl.api.mail.dto.MailResponse;
import com.meetbowl.api.mail.dto.SendAnnouncementRequest;
import com.meetbowl.api.mail.dto.SendMailRequest;
import com.meetbowl.api.mail.dto.SendMailResponse;
import com.meetbowl.application.mail.BackupMailsUseCase;
import com.meetbowl.application.mail.ChangeMailReadStatusUseCase;
import com.meetbowl.application.mail.GetMailDetailUseCase;
import com.meetbowl.application.mail.ListMailUseCase;
import com.meetbowl.application.mail.MoveMailToTrashUseCase;
import com.meetbowl.application.mail.PermanentlyDeleteMailUseCase;
import com.meetbowl.application.mail.RestoreMailUseCase;
import com.meetbowl.application.mail.SearchMailUseCase;
import com.meetbowl.application.mail.SendAnnouncementCommand;
import com.meetbowl.application.mail.SendAnnouncementUseCase;
import com.meetbowl.application.mail.SendMailCommand;
import com.meetbowl.application.mail.SendMailUseCase;
import com.meetbowl.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * 사용자 메일 API Controller다.
 *
 * <p>발송, 받은/보낸/휴지통 조회, 상세, 읽음 변경, 휴지통 이동·복구·영구삭제, 검색, 선택 백업을 제공한다. 공지 발송은 관리자만 가능하다. 인증/권한 검증은
 * 어노테이션과 SecurityConfig가 맡고, 사용자 식별자는 본문이 아닌 @CurrentUser에서만 채워 임의 사용자 사칭을 막는다.
 */
@Validated
@RestController
@RequireUserOrAdmin
@SecurityRequirement(name = ApiHeaders.AUTHORIZATION)
@RequestMapping(ApiPaths.API_V1 + "/mails")
public class MailController extends BaseController {

    private final SendMailUseCase sendMailUseCase;
    private final ListMailUseCase listMailUseCase;
    private final GetMailDetailUseCase getMailDetailUseCase;
    private final ChangeMailReadStatusUseCase changeMailReadStatusUseCase;
    private final MoveMailToTrashUseCase moveMailToTrashUseCase;
    private final RestoreMailUseCase restoreMailUseCase;
    private final PermanentlyDeleteMailUseCase permanentlyDeleteMailUseCase;
    private final BackupMailsUseCase backupMailsUseCase;
    private final SearchMailUseCase searchMailUseCase;
    private final SendAnnouncementUseCase sendAnnouncementUseCase;

    public MailController(
            SendMailUseCase sendMailUseCase,
            ListMailUseCase listMailUseCase,
            GetMailDetailUseCase getMailDetailUseCase,
            ChangeMailReadStatusUseCase changeMailReadStatusUseCase,
            MoveMailToTrashUseCase moveMailToTrashUseCase,
            RestoreMailUseCase restoreMailUseCase,
            PermanentlyDeleteMailUseCase permanentlyDeleteMailUseCase,
            BackupMailsUseCase backupMailsUseCase,
            SearchMailUseCase searchMailUseCase,
            SendAnnouncementUseCase sendAnnouncementUseCase) {
        this.sendMailUseCase = sendMailUseCase;
        this.listMailUseCase = listMailUseCase;
        this.getMailDetailUseCase = getMailDetailUseCase;
        this.changeMailReadStatusUseCase = changeMailReadStatusUseCase;
        this.moveMailToTrashUseCase = moveMailToTrashUseCase;
        this.restoreMailUseCase = restoreMailUseCase;
        this.permanentlyDeleteMailUseCase = permanentlyDeleteMailUseCase;
        this.backupMailsUseCase = backupMailsUseCase;
        this.searchMailUseCase = searchMailUseCase;
        this.sendAnnouncementUseCase = sendAnnouncementUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SendMailResponse>> send(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody SendMailRequest request) {
        SendMailCommand command =
                new SendMailCommand(
                        user.organizationId(),
                        user.userId(),
                        request.recipientUserIds(),
                        request.subject(),
                        request.body(),
                        request.bodyType(),
                        request.relatedResourceType(),
                        request.relatedResourceId(),
                        request.idempotencyKey());
        return created(SendMailResponse.from(sendMailUseCase.execute(command)));
    }

    @GetMapping("/inbox")
    public ApiResponse<MailPageResponse> inbox(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ok(MailPageResponse.from(listMailUseCase.inbox(user.userId(), page, size)));
    }

    @PostMapping("/backup")
    public ApiResponse<List<BackupMailResponse>> backup(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody BackupMailsRequest request) {
        return ok(
                backupMailsUseCase.execute(user.userId(), request.mailIds()).stream()
                        .map(BackupMailResponse::from)
                        .toList());
    }

    @GetMapping("/search")
    public ApiResponse<MailPageResponse> search(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @RequestParam String q,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ok(MailPageResponse.from(searchMailUseCase.execute(user.userId(), q, page, size)));
    }

    @RequireAdmin
    @PostMapping("/announcements")
    public ResponseEntity<ApiResponse<SendMailResponse>> announce(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody SendAnnouncementRequest request) {
        SendAnnouncementCommand command =
                new SendAnnouncementCommand(
                        user.organizationId(),
                        user.userId(),
                        request.subject(),
                        request.body(),
                        request.bodyType(),
                        request.idempotencyKey());
        return created(SendMailResponse.from(sendAnnouncementUseCase.execute(command)));
    }

    @GetMapping("/sent")
    public ApiResponse<MailPageResponse> sent(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ok(MailPageResponse.from(listMailUseCase.sent(user.userId(), page, size)));
    }

    @GetMapping("/trash")
    public ApiResponse<MailPageResponse> trash(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ok(MailPageResponse.from(listMailUseCase.trash(user.userId(), page, size)));
    }

    @GetMapping("/{mailId}")
    public ApiResponse<MailResponse.Detail> detail(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @PathVariable UUID mailId) {
        return ok(MailResponse.Detail.from(getMailDetailUseCase.execute(mailId, user.userId())));
    }

    @PatchMapping("/{mailId}/read")
    public ApiResponse<Void> changeRead(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @PathVariable UUID mailId,
            @Valid @RequestBody ChangeMailReadRequest request) {
        changeMailReadStatusUseCase.execute(mailId, user.userId(), request.read());
        return ok();
    }

    @DeleteMapping("/{mailId}")
    public ApiResponse<Void> moveToTrash(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @PathVariable UUID mailId) {
        moveMailToTrashUseCase.execute(mailId, user.userId());
        return ok();
    }

    @PostMapping("/{mailId}/restore")
    public ApiResponse<Void> restore(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @PathVariable UUID mailId) {
        restoreMailUseCase.execute(mailId, user.userId());
        return ok();
    }

    @DeleteMapping("/{mailId}/permanent")
    public ApiResponse<Void> permanentlyDelete(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @PathVariable UUID mailId) {
        permanentlyDeleteMailUseCase.execute(mailId, user.userId());
        return ok();
    }
}
