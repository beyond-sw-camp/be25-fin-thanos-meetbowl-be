package com.meetbowl.api.personalworkspace;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.RequireUserOrAdmin;
import com.meetbowl.api.personalworkspace.dto.DriveFileResponse;
import com.meetbowl.api.personalworkspace.dto.RegisterDriveFileRequest;
import com.meetbowl.application.personalworkspace.drive.DeleteDriveFileUseCase;
import com.meetbowl.application.personalworkspace.drive.DriveFileResult;
import com.meetbowl.application.personalworkspace.drive.GetDriveFileUseCase;
import com.meetbowl.application.personalworkspace.drive.GetDriveFilesUseCase;
import com.meetbowl.application.personalworkspace.drive.RegisterDriveFileCommand;
import com.meetbowl.application.personalworkspace.drive.RegisterDriveFileUseCase;
import com.meetbowl.common.response.ApiResponse;

/**
 * 개인 워크스페이스 드라이브 파일 API다.
 *
 * <p>현재는 파일 메타데이터 등록/조회/삭제만 제공한다. 실제 바이너리 업로드(multipart)와 다운로드(presigned URL)는 Object Storage 어댑터
 * 연동 후속 작업에서 추가한다.
 */
@RequireUserOrAdmin
@RestController
@RequestMapping(ApiPaths.API_V1 + "/workspace/drive/files")
public class WorkspaceDriveController extends BaseController {

    private final GetDriveFilesUseCase getDriveFilesUseCase;
    private final RegisterDriveFileUseCase registerDriveFileUseCase;
    private final GetDriveFileUseCase getDriveFileUseCase;
    private final DeleteDriveFileUseCase deleteDriveFileUseCase;

    public WorkspaceDriveController(
            GetDriveFilesUseCase getDriveFilesUseCase,
            RegisterDriveFileUseCase registerDriveFileUseCase,
            GetDriveFileUseCase getDriveFileUseCase,
            DeleteDriveFileUseCase deleteDriveFileUseCase) {
        this.getDriveFilesUseCase = getDriveFilesUseCase;
        this.registerDriveFileUseCase = registerDriveFileUseCase;
        this.getDriveFileUseCase = getDriveFileUseCase;
        this.deleteDriveFileUseCase = deleteDriveFileUseCase;
    }

    @GetMapping
    public ApiResponse<List<DriveFileResponse>> getFiles(@CurrentUser AuthenticatedUser user) {
        List<DriveFileResult> results = getDriveFilesUseCase.execute(user.userId());
        return ok(results.stream().map(DriveFileResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DriveFileResponse>> registerFile(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody RegisterDriveFileRequest request) {
        DriveFileResult result =
                registerDriveFileUseCase.execute(
                        new RegisterDriveFileCommand(
                                user.userId(),
                                request.originalFileName(),
                                request.contentType(),
                                request.sizeBytes()));
        return created(DriveFileResponse.from(result));
    }

    @GetMapping("/{fileId}")
    public ApiResponse<DriveFileResponse> getFile(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID fileId) {
        DriveFileResult result = getDriveFileUseCase.execute(user.userId(), fileId);
        return ok(DriveFileResponse.from(result));
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> deleteFile(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID fileId) {
        deleteDriveFileUseCase.execute(user.userId(), fileId);
        return ok();
    }
}
