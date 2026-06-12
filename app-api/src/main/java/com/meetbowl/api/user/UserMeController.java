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
import com.meetbowl.api.user.dto.MyMenusResponse;
import com.meetbowl.api.user.dto.MyProfileResponse;
import com.meetbowl.api.user.dto.MyProfileUpdateRequest;
import com.meetbowl.api.user.dto.MySettingsResponse;
import com.meetbowl.api.user.dto.MySettingsUpdateRequest;
import com.meetbowl.application.user.GetMyMenusUseCase;
import com.meetbowl.application.user.MyProfileUseCase;
import com.meetbowl.application.user.MySettingsUseCase;
import com.meetbowl.application.user.UpdateMyProfileCommand;
import com.meetbowl.common.response.ApiResponse;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/users/me")
public class UserMeController extends BaseController {

    private final MyProfileUseCase myProfileUseCase;
    private final MySettingsUseCase mySettingsUseCase;
    private final GetMyMenusUseCase getMyMenusUseCase;
    private final GlobalPermissionChecker globalPermissionChecker;

    public UserMeController(
            MyProfileUseCase myProfileUseCase,
            MySettingsUseCase mySettingsUseCase,
            GetMyMenusUseCase getMyMenusUseCase,
            GlobalPermissionChecker globalPermissionChecker) {
        this.myProfileUseCase = myProfileUseCase;
        this.mySettingsUseCase = mySettingsUseCase;
        this.getMyMenusUseCase = getMyMenusUseCase;
        this.globalPermissionChecker = globalPermissionChecker;
    }

    @GetMapping
    public ApiResponse<MyProfileResponse> getProfile(@CurrentUser AuthenticatedUser currentUser) {
        requireUserOrAdmin(currentUser);
        return ok(MyProfileResponse.from(myProfileUseCase.get(currentUser.userId())));
    }

    @GetMapping("/menus")
    public ApiResponse<MyMenusResponse> getMenus(@CurrentUser AuthenticatedUser currentUser) {
        // 메뉴 API는 화면 노출 제어용이지만, 조회 가능 역할 자체는 백엔드에서 동일하게 제한한다.
        requireUserOrAdmin(currentUser);
        return ok(MyMenusResponse.from(getMyMenusUseCase.get(currentUser.role().name())));
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
                                new MySettingsUseCase.UpdateMySettingsCommand(
                                        currentUser.userId(),
                                        request.meetingStartReminderMinutes(),
                                        request.autoBackupEnabled(),
                                        request.autoBackupTime()))));
    }

    private void requireUserOrAdmin(AuthenticatedUser currentUser) {
        // USER와 ADMIN 공용 진입 규칙을 한 곳에 두어 /users/me 계열 API가 동일하게 동작하게 맞춘다.
        globalPermissionChecker.requireUserOrAdmin(currentUser);
    }
}
