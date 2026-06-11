package com.meetbowl.api.personalworkspace;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.RequireUserOrAdmin;
import com.meetbowl.api.personalworkspace.dto.BackupResponse;
import com.meetbowl.application.personalworkspace.backup.AddBackupBookmarkUseCase;
import com.meetbowl.application.personalworkspace.backup.BackupResult;
import com.meetbowl.application.personalworkspace.backup.GetBackupsUseCase;
import com.meetbowl.application.personalworkspace.backup.RemoveBackupBookmarkUseCase;
import com.meetbowl.application.personalworkspace.backup.SearchBackupsUseCase;
import com.meetbowl.common.response.ApiResponse;

@RequireUserOrAdmin
@RestController
@RequestMapping(ApiPaths.API_V1 + "/workspace/backups")
public class WorkspaceBackupController extends BaseController {

    private final GetBackupsUseCase getBackupsUseCase;
    private final SearchBackupsUseCase searchBackupsUseCase;
    private final AddBackupBookmarkUseCase addBackupBookmarkUseCase;
    private final RemoveBackupBookmarkUseCase removeBackupBookmarkUseCase;

    public WorkspaceBackupController(
            GetBackupsUseCase getBackupsUseCase,
            SearchBackupsUseCase searchBackupsUseCase,
            AddBackupBookmarkUseCase addBackupBookmarkUseCase,
            RemoveBackupBookmarkUseCase removeBackupBookmarkUseCase) {
        this.getBackupsUseCase = getBackupsUseCase;
        this.searchBackupsUseCase = searchBackupsUseCase;
        this.addBackupBookmarkUseCase = addBackupBookmarkUseCase;
        this.removeBackupBookmarkUseCase = removeBackupBookmarkUseCase;
    }

    @GetMapping
    public ApiResponse<List<BackupResponse>> getBackups(@CurrentUser AuthenticatedUser user) {
        List<BackupResult> results = getBackupsUseCase.execute(user.userId());
        return ok(results.stream().map(BackupResponse::from).toList());
    }

    @GetMapping("/search")
    public ApiResponse<List<BackupResponse>> searchBackups(
            @CurrentUser AuthenticatedUser user, @RequestParam String keyword) {
        List<BackupResult> results = searchBackupsUseCase.execute(user.userId(), keyword);
        return ok(results.stream().map(BackupResponse::from).toList());
    }

    @PostMapping("/{backupId}/bookmark")
    public ApiResponse<Void> addBookmark(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID backupId) {
        addBackupBookmarkUseCase.execute(user.userId(), backupId);
        return ok();
    }

    @DeleteMapping("/{backupId}/bookmark")
    public ApiResponse<Void> removeBookmark(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID backupId) {
        removeBackupBookmarkUseCase.execute(user.userId(), backupId);
        return ok();
    }
}
