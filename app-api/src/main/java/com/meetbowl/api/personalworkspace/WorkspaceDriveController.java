package com.meetbowl.api.personalworkspace;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.RequireUserOrAdmin;
import com.meetbowl.api.personalworkspace.dto.DriveFileResponse;
import com.meetbowl.application.personalworkspace.drive.DeleteDriveFileUseCase;
import com.meetbowl.application.personalworkspace.drive.DownloadDriveFileUseCase;
import com.meetbowl.application.personalworkspace.drive.DriveFileDownloadResult;
import com.meetbowl.application.personalworkspace.drive.DriveFileResult;
import com.meetbowl.application.personalworkspace.drive.GetDriveFileUseCase;
import com.meetbowl.application.personalworkspace.drive.GetDriveFilesUseCase;
import com.meetbowl.application.personalworkspace.drive.RegisterDriveFileCommand;
import com.meetbowl.application.personalworkspace.drive.RegisterDriveFileUseCase;
import com.meetbowl.common.response.ApiResponse;

/**
 * 개인 워크스페이스 드라이브 파일 API다.
 *
 * <p>업로드 파일은 서버가 검증한 뒤 Object Storage에 저장하고 DB에는 메타데이터만 남긴다. 원본 다운로드와 미리보기는 소유자 검증 후 서버가 S3에서 읽어
 * 내려준다.
 */
@RequireUserOrAdmin
@RestController
@RequestMapping(ApiPaths.API_V1 + "/workspace/drive/files")
public class WorkspaceDriveController extends BaseController {

    private final GetDriveFilesUseCase getDriveFilesUseCase;
    private final RegisterDriveFileUseCase registerDriveFileUseCase;
    private final GetDriveFileUseCase getDriveFileUseCase;
    private final DownloadDriveFileUseCase downloadDriveFileUseCase;
    private final DeleteDriveFileUseCase deleteDriveFileUseCase;

    public WorkspaceDriveController(
            GetDriveFilesUseCase getDriveFilesUseCase,
            RegisterDriveFileUseCase registerDriveFileUseCase,
            GetDriveFileUseCase getDriveFileUseCase,
            DownloadDriveFileUseCase downloadDriveFileUseCase,
            DeleteDriveFileUseCase deleteDriveFileUseCase) {
        this.getDriveFilesUseCase = getDriveFilesUseCase;
        this.registerDriveFileUseCase = registerDriveFileUseCase;
        this.getDriveFileUseCase = getDriveFileUseCase;
        this.downloadDriveFileUseCase = downloadDriveFileUseCase;
        this.deleteDriveFileUseCase = deleteDriveFileUseCase;
    }

    @GetMapping
    public ApiResponse<List<DriveFileResponse>> getFiles(@CurrentUser AuthenticatedUser user) {
        List<DriveFileResult> results = getDriveFilesUseCase.execute(user.userId());
        return ok(results.stream().map(DriveFileResponse::from).toList());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DriveFileResponse>> registerFile(
            @CurrentUser AuthenticatedUser user, @RequestPart("file") MultipartFile file)
            throws IOException {
        DriveFileResult result =
                registerDriveFileUseCase.execute(
                        new RegisterDriveFileCommand(
                                user.userId(),
                                user.organizationId(),
                                file.getOriginalFilename(),
                                file.getBytes()));
        return created(DriveFileResponse.from(result));
    }

    @GetMapping("/{fileId}")
    public ApiResponse<DriveFileResponse> getFile(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID fileId) {
        DriveFileResult result = getDriveFileUseCase.execute(user.userId(), fileId);
        return ok(DriveFileResponse.from(result));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<StreamingResponseBody> downloadFile(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID fileId) {
        DriveFileDownloadResult result = downloadDriveFileUseCase.execute(user.userId(), fileId);
        return fileResponse(result, ContentDisposition.attachment());
    }

    @GetMapping("/{fileId}/preview")
    public ResponseEntity<StreamingResponseBody> previewFile(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID fileId) {
        DriveFileDownloadResult result = downloadDriveFileUseCase.execute(user.userId(), fileId);
        return fileResponse(result, ContentDisposition.inline());
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> deleteFile(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID fileId) {
        deleteDriveFileUseCase.execute(user.userId(), fileId);
        return ok();
    }

    private ResponseEntity<StreamingResponseBody> fileResponse(
            DriveFileDownloadResult result, ContentDisposition.Builder dispositionBuilder) {
        // 원본을 메모리에 모으지 않고 S3 스트림을 응답으로 그대로 흘려보낸 뒤 닫는다.
        StreamingResponseBody body =
                outputStream -> {
                    try (InputStream content = result.content()) {
                        content.transferTo(outputStream);
                    }
                };
        // 한글 파일명은 RFC 5987 방식으로 인코딩해 브라우저 저장 이름이 깨지지 않게 한다.
        ResponseEntity.BodyBuilder builder =
                ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(result.contentType()))
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                dispositionBuilder
                                        .filename(result.originalFileName(), StandardCharsets.UTF_8)
                                        .build()
                                        .toString());
        // 길이를 알 수 없으면(헤더 부재) 청크 전송으로 두고, 알 때만 Content-Length를 명시한다.
        if (result.sizeBytes() > 0) {
            builder.contentLength(result.sizeBytes());
        }
        return builder.body(body);
    }
}
