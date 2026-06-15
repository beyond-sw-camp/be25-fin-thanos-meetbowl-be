package com.meetbowl.api.user;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.api.user.dto.MyProfileResponse;
import com.meetbowl.api.user.dto.MyProfileUpdateRequest;
import com.meetbowl.api.user.dto.MySettingsResponse;
import com.meetbowl.api.user.dto.MySettingsUpdateRequest;
import com.meetbowl.application.user.MyProfileUseCase;
import com.meetbowl.application.user.MySettingsUseCase;
import com.meetbowl.application.user.UpdateMyProfileCommand;
import com.meetbowl.common.response.ApiResponse;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/users/me")
public class UserMeController extends BaseController {

    private final MyProfileUseCase myProfileUseCase;
    private final MySettingsUseCase mySettingsUseCase;
    private final GlobalPermissionChecker globalPermissionChecker;

    public UserMeController(
            MyProfileUseCase myProfileUseCase,
            MySettingsUseCase mySettingsUseCase,
            GlobalPermissionChecker globalPermissionChecker) {
        this.myProfileUseCase = myProfileUseCase;
        this.mySettingsUseCase = mySettingsUseCase;
        this.globalPermissionChecker = globalPermissionChecker;
    }

    @GetMapping
    public ApiResponse<MyProfileResponse> getProfile(@CurrentUser AuthenticatedUser currentUser) {
        requireUserOrAdmin(currentUser);
        return ok(MyProfileResponse.from(myProfileUseCase.get(currentUser.userId())));
    }

    @PatchMapping
    public ApiResponse<MyProfileResponse> updateProfile(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody MyProfileUpdateRequest request) {
        requireUserOrAdmin(currentUser);
        return ok(
                MyProfileResponse.from(
                        myProfileUseCase.update(
                                new UpdateMyProfileCommand(
                                        currentUser.userId(), request.name(), request.email()))));
    }

    @GetMapping("/settings")
    public ApiResponse<MySettingsResponse> getSettings(@CurrentUser AuthenticatedUser currentUser) {
        requireUserOrAdmin(currentUser);
        return ok(MySettingsResponse.from(mySettingsUseCase.get(currentUser.userId())));
    }

    @PatchMapping("/settings")
    public ApiResponse<MySettingsResponse> updateSettings(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody MySettingsUpdateRequest request) {
        requireUserOrAdmin(currentUser);
        return ok(
                MySettingsResponse.from(
                        mySettingsUseCase.update(
                                // 개인 설정 수정은 현재 로그인한 사용자 본인 설정만 갱신한다.
                                new MySettingsUseCase.UpdateMySettingsCommand(
                                        currentUser.userId(),
                                        request.meetingStartReminderMinutes(),
                                        request.minutesReviewReminderMinutes(),
                                        request.autoBackupEnabled(),
                                        request.autoBackupTime()))));
    }

    private void requireUserOrAdmin(AuthenticatedUser currentUser) {
        globalPermissionChecker.requireUserOrAdmin(currentUser);
    }
}
