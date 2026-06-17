package com.meetbowl.api.sharedworkspace;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.RequireUserOrAdmin;
import com.meetbowl.api.sharedworkspace.dto.SharedWorkspaceFileResponse;
import com.meetbowl.api.sharedworkspace.dto.SharedWorkspaceFileVersionResponse;
import com.meetbowl.api.sharedworkspace.dto.UpdateSharedWorkspaceFileVersionMemoRequest;
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
 * 공유 자료와 버전 이력 API다. 모든 자료 접근은 멤버 권한을 요구한다.
 *
 * <p>업로드와 새 버전 업로드는 multipart로 파일 원본을 받아 S3에 저장하고, 저장 후 AI 색인 이벤트를 발행한다. 다운로드는 권한 검증을 통과한 뒤 저장 경로
 * 메타데이터까지만 노출하며, 실제 원본 스트림 전송(presigned URL 등)은 후속 작업으로 남겨둔다.
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SharedWorkspaceFileResponse>> uploadFile(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID spaceId,
            @RequestPart("file") MultipartFile file)
            throws IOException {
        SharedWorkspaceFileResponse response =
                SharedWorkspaceFileResponse.from(
                        uploadSharedWorkspaceFileUseCase.execute(
                                new UploadSharedWorkspaceFileCommand(
                                        spaceId,
                                        user.userId(),
                                        user.organizationId(),
                                        file.getOriginalFilename(),
                                        file.getBytes())));
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

    @PostMapping(value = "/{fileId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SharedWorkspaceFileVersionResponse>> addVersion(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID spaceId,
            @PathVariable UUID fileId,
            @RequestPart("file") MultipartFile file,
            @RequestParam("expectedCurrentVersion") String expectedCurrentVersion,
            @RequestParam("newVersion") String newVersion,
            @RequestParam(value = "changeMemo", required = false) String changeMemo)
            throws IOException {
        SharedWorkspaceFileVersionResponse response =
                SharedWorkspaceFileVersionResponse.from(
                        addSharedWorkspaceFileVersionUseCase.execute(
                                new AddSharedWorkspaceFileVersionCommand(
                                        spaceId,
                                        fileId,
                                        user.userId(),
                                        user.organizationId(),
                                        file.getOriginalFilename(),
                                        file.getBytes(),
                                        expectedCurrentVersion,
                                        newVersion,
                                        changeMemo)));
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
