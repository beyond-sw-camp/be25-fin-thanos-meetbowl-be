package com.meetbowl.api.minutes;

import java.util.UUID;

import jakarta.validation.Valid;

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
import com.meetbowl.application.minutes.ApproveMinutesCommand;
import com.meetbowl.application.minutes.ApproveMinutesUseCase;
import com.meetbowl.application.minutes.GetMinutesUseCase;
import com.meetbowl.application.minutes.ReviseMinutesCommand;
import com.meetbowl.application.minutes.ReviseMinutesUseCase;
import com.meetbowl.application.minutes.ShareMinutesCommand;
import com.meetbowl.application.minutes.ShareMinutesUseCase;
import com.meetbowl.common.response.ApiResponse;

/** 회의별 회의록 수정과 승인 HTTP 요청을 application UseCase로 전달한다. */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/meetings/{meetingId}/minutes")
public class MinutesController extends BaseController {

    private final GetMinutesUseCase getMinutesUseCase;
    private final ReviseMinutesUseCase reviseMinutesUseCase;
    private final ApproveMinutesUseCase approveMinutesUseCase;
    private final ShareMinutesUseCase shareMinutesUseCase;

    public MinutesController(
            GetMinutesUseCase getMinutesUseCase,
            ReviseMinutesUseCase reviseMinutesUseCase,
            ApproveMinutesUseCase approveMinutesUseCase,
            ShareMinutesUseCase shareMinutesUseCase) {
        this.getMinutesUseCase = getMinutesUseCase;
        this.reviseMinutesUseCase = reviseMinutesUseCase;
        this.approveMinutesUseCase = approveMinutesUseCase;
        this.shareMinutesUseCase = shareMinutesUseCase;
    }

    @GetMapping
    public ApiResponse<MinutesResponse> get(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID meetingId) {
        return ok(
                MinutesResponse.from(
                        getMinutesUseCase.get(
                                meetingId, user.userId(), user.organizationId(), user.isAdmin())));
    }

    @PatchMapping
    public ApiResponse<MinutesResponse> revise(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID meetingId,
            @Valid @RequestBody ReviseMinutesRequest request) {
        // Controller는 HTTP 입력을 Command로 변환만 한다. 검토자·조직·상태 검증은 UseCase와 Domain에 둔다.
        return ok(
                MinutesResponse.from(
                        reviseMinutesUseCase.execute(
                                new ReviseMinutesCommand(
                                        meetingId,
                                        user.userId(),
                                        user.organizationId(),
                                        request.summary(),
                                        request.content()))));
    }

    @PostMapping("/approve")
    public ApiResponse<MinutesResponse> approve(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID meetingId) {
        // 승인 시각과 승인 가능 상태는 요청 값으로 받지 않고 서버 내부에서 확정한다.
        return ok(
                MinutesResponse.from(
                        approveMinutesUseCase.execute(
                                new ApproveMinutesCommand(
                                        meetingId, user.userId(), user.organizationId()))));
    }

    @PostMapping("/share")
    public ApiResponse<MinutesResponse> share(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID meetingId,
            @Valid @RequestBody ShareMinutesRequest request) {
        return ok(
                MinutesResponse.from(
                        shareMinutesUseCase.execute(
                                new ShareMinutesCommand(
                                        meetingId,
                                        user.userId(),
                                        user.organizationId(),
                                        request.recipientUserIds(),
                                        request.subject(),
                                        request.body(),
                                        request.idempotencyKey()))));
    }
}
