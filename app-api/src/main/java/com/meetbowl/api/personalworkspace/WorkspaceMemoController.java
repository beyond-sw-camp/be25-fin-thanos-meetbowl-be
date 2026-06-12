package com.meetbowl.api.personalworkspace;

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
import com.meetbowl.api.personalworkspace.dto.CreateMemoRequest;
import com.meetbowl.api.personalworkspace.dto.MemoResponse;
import com.meetbowl.api.personalworkspace.dto.UpdateMemoRequest;
import com.meetbowl.application.personalworkspace.memo.CreateMemoCommand;
import com.meetbowl.application.personalworkspace.memo.CreateMemoUseCase;
import com.meetbowl.application.personalworkspace.memo.DeleteMemoUseCase;
import com.meetbowl.application.personalworkspace.memo.GetMemosUseCase;
import com.meetbowl.application.personalworkspace.memo.MemoResult;
import com.meetbowl.application.personalworkspace.memo.UpdateMemoCommand;
import com.meetbowl.application.personalworkspace.memo.UpdateMemoUseCase;
import com.meetbowl.common.response.ApiResponse;

/**
 * 개인 워크스페이스 메모 API Controller다.
 *
 * <p>개인 메모 작성/수정/삭제와 목록 조회를 제공한다. 메모는 사용자 본인 소유 자료이므로 모든 동작을 현재 사용자 기준으로만 수행한다.
 */
@RequireUserOrAdmin
@RestController
@RequestMapping(ApiPaths.API_V1 + "/workspace/memos")
public class WorkspaceMemoController extends BaseController {

    private final GetMemosUseCase getMemosUseCase;
    private final CreateMemoUseCase createMemoUseCase;
    private final UpdateMemoUseCase updateMemoUseCase;
    private final DeleteMemoUseCase deleteMemoUseCase;

    public WorkspaceMemoController(
            GetMemosUseCase getMemosUseCase,
            CreateMemoUseCase createMemoUseCase,
            UpdateMemoUseCase updateMemoUseCase,
            DeleteMemoUseCase deleteMemoUseCase) {
        this.getMemosUseCase = getMemosUseCase;
        this.createMemoUseCase = createMemoUseCase;
        this.updateMemoUseCase = updateMemoUseCase;
        this.deleteMemoUseCase = deleteMemoUseCase;
    }

    @GetMapping
    public ApiResponse<List<MemoResponse>> getMemos(@CurrentUser AuthenticatedUser user) {
        List<MemoResult> results = getMemosUseCase.execute(user.userId());
        return ok(results.stream().map(MemoResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MemoResponse>> createMemo(
            @CurrentUser AuthenticatedUser user, @Valid @RequestBody CreateMemoRequest request) {
        MemoResult result =
                createMemoUseCase.execute(
                        new CreateMemoCommand(user.userId(), request.title(), request.content()));
        return created(MemoResponse.from(result));
    }

    @PatchMapping("/{memoId}")
    public ApiResponse<MemoResponse> updateMemo(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID memoId,
            @Valid @RequestBody UpdateMemoRequest request) {
        MemoResult result =
                updateMemoUseCase.execute(
                        new UpdateMemoCommand(
                                memoId, user.userId(), request.title(), request.content()));
        return ok(MemoResponse.from(result));
    }

    @DeleteMapping("/{memoId}")
    public ApiResponse<Void> deleteMemo(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID memoId) {
        deleteMemoUseCase.execute(user.userId(), memoId);
        return ok();
    }
}
