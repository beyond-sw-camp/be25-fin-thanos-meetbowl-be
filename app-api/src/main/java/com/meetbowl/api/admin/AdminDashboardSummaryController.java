package com.meetbowl.api.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.admin.dto.AdminDashboardSummaryResponse;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.application.admin.AdminDashboardSummaryUseCase;
import com.meetbowl.common.response.ApiResponse;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/admin/dashboard/summary")
public class AdminDashboardSummaryController extends BaseController {

    private final AdminDashboardSummaryUseCase adminDashboardSummaryUseCase;
    private final GlobalPermissionChecker globalPermissionChecker;

    public AdminDashboardSummaryController(
            AdminDashboardSummaryUseCase adminDashboardSummaryUseCase,
            GlobalPermissionChecker globalPermissionChecker) {
        this.adminDashboardSummaryUseCase = adminDashboardSummaryUseCase;
        this.globalPermissionChecker = globalPermissionChecker;
    }

    @GetMapping
    public ApiResponse<AdminDashboardSummaryResponse> get(@CurrentUser AuthenticatedUser admin) {
        requireAdmin(admin);
        return ok(AdminDashboardSummaryResponse.from(adminDashboardSummaryUseCase.get()));
    }

    private void requireAdmin(AuthenticatedUser admin) {
        globalPermissionChecker.requireAdmin(admin);
    }
}
