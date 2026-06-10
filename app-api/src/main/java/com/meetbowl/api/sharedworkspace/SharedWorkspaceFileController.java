package com.meetbowl.api.sharedworkspace;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import com.meetbowl.api.sharedworkspace.dto.AddSharedWorkspaceFileVersionRequest;
import com.meetbowl.api.sharedworkspace.dto.SharedWorkspaceFileResponse;
import com.meetbowl.api.sharedworkspace.dto.SharedWorkspaceFileVersionResponse;
import com.meetbowl.api.sharedworkspace.dto.UpdateSharedWorkspaceFileVersionMemoRequest;
import com.meetbowl.api.sharedworkspace.dto.UploadSharedWorkspaceFileRequest;
import com.meetbowl.application.sharedworkspace.AddSharedWorkspaceFileVersionCommand;
import com.meetbowl.application.sharedworkspace.AddSharedWorkspaceFileVersionUseCase;
import com.meetbowl.application.sharedworkspace.DeleteSharedWorkspaceFileUseCase;
import com.meetbowl.application.sharedworkspace.DownloadSharedWorkspaceFileUseCase;
import com.meetbowl.application.sharedworkspace.GetSharedWorkspaceFileVersionsUseCase;
import com.meetbowl.application.sharedworkspace.GetSharedWorkspaceFilesUseCase;
import com.meetbowl.application.sharedworkspace.UpdateSharedWorkspaceFileVersionMemoCommand;
import com.meetbowl.application.sharedworkspace.UpdateSharedWorkspaceFileVersionMemoUseCase;
import com.meetbowl.application.sharedworkspace.UploadSharedWorkspaceFileCommand;
import com.meetbowl.application.sharedworkspace.UploadSharedWorkspaceFileUseCase;
import com.meetbowl.common.response.ApiResponse;

/**
 * 공유 자료와 버전 이력 API다. 모든 자료 접근은 멤버 권한을 요구하고, 다운로드는 권한 검증을 통과한 뒤에만 저장 경로 메타데이터를 노출한다. 실제 원본 스트림 전송은
 * Object Storage 연동(후속 작업)이 담당하므로 여기서는 메타데이터까지만 응답한다.
 */
@RequireUserOrAdmin
@RestController
@RequestMapping(ApiPaths.API_V1 + "/shared-workspaces/{spaceId}/files")
public class SharedWorkspaceFileController extends BaseController {

    private final GetSharedWorkspaceFilesUseCase getSharedWorkspaceFilesUseCase;
    private final UploadSharedWorkspaceFileUseCase uploadSharedWorkspaceFileUseCase;
    private final DownloadSharedWorkspaceFileUseCase downloadSharedWorkspaceFileUseCase;
    private final DeleteSharedWorkspaceFileUseCase deleteSharedWorkspaceFileUseCase;
    private final AddSharedWorkspaceFileVersionUseCase addSharedWorkspaceFileVersionUseCase;
    private final GetSharedWorkspaceFileVersionsUseCase getSharedWorkspaceFileVersionsUseCase;
    private final UpdateSharedWorkspaceFileVersionMemoUseCase
            updateSharedWorkspaceFileVersionMemoUseCase;

    public SharedWorkspaceFileController(
            GetSharedWorkspaceFilesUseCase getSharedWorkspaceFilesUseCase,
            UploadSharedWorkspaceFileUseCase uploadSharedWorkspaceFileUseCase,
            DownloadSharedWorkspaceFileUseCase downloadSharedWorkspaceFileUseCase,
            DeleteSharedWorkspaceFileUseCase deleteSharedWorkspaceFileUseCase,
            AddSharedWorkspaceFileVersionUseCase addSharedWorkspaceFileVersionUseCase,
            GetSharedWorkspaceFileVersionsUseCase getSharedWorkspaceFileVersionsUseCase,
            UpdateSharedWorkspaceFileVersionMemoUseCase
                    updateSharedWorkspaceFileVersionMemoUseCase) {
        this.getSharedWorkspaceFilesUseCase = getSharedWorkspaceFilesUseCase;
        this.uploadSharedWorkspaceFileUseCase = uploadSharedWorkspaceFileUseCase;
        this.downloadSharedWorkspaceFileUseCase = downloadSharedWorkspaceFileUseCase;
        this.deleteSharedWorkspaceFileUseCase = deleteSharedWorkspaceFileUseCase;
        this.addSharedWorkspaceFileVersionUseCase = addSharedWorkspaceFileVersionUseCase;
        this.getSharedWorkspaceFileVersionsUseCase = getSharedWorkspaceFileVersionsUseCase;
        this.updateSharedWorkspaceFileVersionMemoUseCase =
                updateSharedWorkspaceFileVersionMemoUseCase;
    }

    @GetMapping
    public ApiResponse<List<SharedWorkspaceFileResponse>> getFiles(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID spaceId) {
        List<SharedWorkspaceFileResponse> files =
                getSharedWorkspaceFilesUseCase.execute(spaceId, user.userId()).stream()
                        .map(SharedWorkspaceFileResponse::from)
                        .toList();
        return ok(files);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SharedWorkspaceFileResponse>> uploadFile(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID spaceId,
            @Valid @RequestBody UploadSharedWorkspaceFileRequest request) {
        SharedWorkspaceFileResponse response =
                SharedWorkspaceFileResponse.from(
                        uploadSharedWorkspaceFileUseCase.execute(
                                new UploadSharedWorkspaceFileCommand(
                                        spaceId,
                                        user.userId(),
                                        request.originalFileName(),
                                        request.contentType(),
                                        request.sizeBytes(),
                                        request.storageKey())));
        return created(response);
    }

    @GetMapping("/{fileId}")
    public ApiResponse<SharedWorkspaceFileResponse> downloadFile(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID spaceId,
            @PathVariable UUID fileId) {
        return ok(
                SharedWorkspaceFileResponse.from(
                        downloadSharedWorkspaceFileUseCase.execute(
                                spaceId, fileId, user.userId())));
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> deleteFile(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID spaceId,
            @PathVariable UUID fileId) {
        deleteSharedWorkspaceFileUseCase.execute(spaceId, fileId, user.userId());
        return ok();
    }

    @PostMapping("/{fileId}/versions")
    public ResponseEntity<ApiResponse<SharedWorkspaceFileVersionResponse>> addVersion(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID spaceId,
            @PathVariable UUID fileId,
            @Valid @RequestBody AddSharedWorkspaceFileVersionRequest request) {
        SharedWorkspaceFileVersionResponse response =
                SharedWorkspaceFileVersionResponse.from(
                        addSharedWorkspaceFileVersionUseCase.execute(
                                new AddSharedWorkspaceFileVersionCommand(
                                        spaceId,
                                        fileId,
                                        user.userId(),
                                        request.originalFileName(),
                                        request.contentType(),
                                        request.sizeBytes(),
                                        request.storageKey(),
                                        request.expectedCurrentVersion(),
                                        request.newVersion(),
                                        request.changeMemo())));
        return created(response);
    }

    @GetMapping("/{fileId}/versions")
    public ApiResponse<List<SharedWorkspaceFileVersionResponse>> getVersions(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID spaceId,
            @PathVariable UUID fileId) {
        List<SharedWorkspaceFileVersionResponse> versions =
                getSharedWorkspaceFileVersionsUseCase
                        .execute(spaceId, fileId, user.userId())
                        .stream()
                        .map(SharedWorkspaceFileVersionResponse::from)
                        .toList();
        return ok(versions);
    }

    @PatchMapping("/{fileId}/versions/{versionId}")
    public ApiResponse<SharedWorkspaceFileVersionResponse> updateVersionMemo(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID spaceId,
            @PathVariable UUID fileId,
            @PathVariable UUID versionId,
            @Valid @RequestBody UpdateSharedWorkspaceFileVersionMemoRequest request) {
        return ok(
                SharedWorkspaceFileVersionResponse.from(
                        updateSharedWorkspaceFileVersionMemoUseCase.execute(
                                new UpdateSharedWorkspaceFileVersionMemoCommand(
                                        spaceId,
                                        fileId,
                                        versionId,
                                        user.userId(),
                                        request.changeMemo()))));
    }
}
