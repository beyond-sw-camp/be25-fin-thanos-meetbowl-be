package com.meetbowl.api.mail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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
import com.meetbowl.application.mail.DownloadMailAttachmentUseCase;
import com.meetbowl.application.mail.MailAttachmentDownloadResult;
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
    private final DownloadMailAttachmentUseCase downloadMailAttachmentUseCase;

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
            SendAnnouncementUseCase sendAnnouncementUseCase,
            DownloadMailAttachmentUseCase downloadMailAttachmentUseCase) {
        this.downloadMailAttachmentUseCase = downloadMailAttachmentUseCase;
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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

    // 첨부가 있는 메일은 본문 필드 + 파일을 multipart로 함께 받아 한 트랜잭션으로 발송한다(첨부는 DRAFT 단계에서만 등록 가능).
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SendMailResponse>> sendWithAttachments(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @RequestParam List<UUID> recipientUserIds,
            @RequestParam String subject,
            @RequestParam String body,
            @RequestParam String bodyType,
            @RequestParam(required = false) String relatedResourceType,
            @RequestParam(required = false) UUID relatedResourceId,
            @RequestParam UUID idempotencyKey,
            @RequestPart(required = false) List<MultipartFile> files) {
        List<SendMailCommand.AttachmentUpload> attachments = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                attachments.add(
                        new SendMailCommand.AttachmentUpload(
                                file.getOriginalFilename(), file.getContentType(), readBytes(file)));
            }
        }
        SendMailCommand command =
                new SendMailCommand(
                        user.organizationId(),
                        user.userId(),
                        recipientUserIds,
                        subject,
                        body,
                        bodyType,
                        relatedResourceType,
                        relatedResourceId,
                        idempotencyKey,
                        attachments);
        return created(SendMailResponse.from(sendMailUseCase.execute(command)));
    }

    @GetMapping("/{mailId}/attachments/{attachmentId}")
    public ResponseEntity<StreamingResponseBody> downloadAttachment(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user,
            @PathVariable UUID mailId,
            @PathVariable UUID attachmentId) {
        MailAttachmentDownloadResult result =
                downloadMailAttachmentUseCase.execute(mailId, attachmentId, user.userId());
        StreamingResponseBody bodyStream =
                outputStream -> {
                    try (InputStream content = result.content()) {
                        content.transferTo(outputStream);
                    }
                };
        ResponseEntity.BodyBuilder builder =
                ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(result.contentType()))
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.attachment()
                                        .filename(result.originalFileName(), StandardCharsets.UTF_8)
                                        .build()
                                        .toString());
        if (result.sizeBytes() > 0) {
            builder.contentLength(result.sizeBytes());
        }
        return builder.body(bodyStream);
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new com.meetbowl.common.exception.BusinessException(
                    com.meetbowl.common.exception.ErrorCode.COMMON_INTERNAL_ERROR,
                    "첨부파일을 읽지 못했습니다.");
        }
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
                backupMailsUseCase
                        .execute(user.userId(), user.organizationId(), request.mailIds())
                        .stream()
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
