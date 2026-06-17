package com.meetbowl.api.minutes;

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
import com.meetbowl.application.minutes.AddMinutesFavoriteUseCase;
import com.meetbowl.application.minutes.GetMinutesListUseCase;
import com.meetbowl.application.minutes.RemoveMinutesFavoriteUseCase;
import com.meetbowl.common.response.ApiResponse;

/** 개인 워크스페이스에서 사용할 회의록 목록과 즐겨찾기 HTTP 요청을 처리한다. */
@RequireUserOrAdmin
@RestController
@RequestMapping(ApiPaths.API_V1 + "/minutes")
public class MinutesListController extends BaseController {

    private final GetMinutesListUseCase getMinutesListUseCase;
    private final AddMinutesFavoriteUseCase addMinutesFavoriteUseCase;
    private final RemoveMinutesFavoriteUseCase removeMinutesFavoriteUseCase;

    public MinutesListController(
            GetMinutesListUseCase getMinutesListUseCase,
            AddMinutesFavoriteUseCase addMinutesFavoriteUseCase,
            RemoveMinutesFavoriteUseCase removeMinutesFavoriteUseCase) {
        this.getMinutesListUseCase = getMinutesListUseCase;
        this.addMinutesFavoriteUseCase = addMinutesFavoriteUseCase;
        this.removeMinutesFavoriteUseCase = removeMinutesFavoriteUseCase;
    }

    @GetMapping
    public ApiResponse<List<MinutesListItemResponse>> getMinutes(
            @CurrentUser AuthenticatedUser user, @RequestParam(required = false) String keyword) {
        return ok(
                getMinutesListUseCase
                        .execute(user.userId(), user.organizationId(), keyword)
                        .stream()
                        .map(MinutesListItemResponse::from)
                        .toList());
    }

    @PostMapping("/{minutesId}/favorite")
    public ApiResponse<Void> addFavorite(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID minutesId) {
        addMinutesFavoriteUseCase.execute(user.userId(), user.organizationId(), minutesId);
        return ok();
    }

    @DeleteMapping("/{minutesId}/favorite")
    public ApiResponse<Void> removeFavorite(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID minutesId) {
        removeMinutesFavoriteUseCase.execute(user.userId(), user.organizationId(), minutesId);
        return ok();
    }
}
