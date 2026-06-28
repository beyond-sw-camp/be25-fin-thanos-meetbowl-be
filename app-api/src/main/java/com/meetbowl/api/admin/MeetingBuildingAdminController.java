package com.meetbowl.api.admin;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.RequireAdmin;
import com.meetbowl.application.meetingroom.BuildingAdminUseCase;
import com.meetbowl.common.response.ApiResponse;

/** 관리자 건물 기준정보 관리 API다(F1, FR-095). Admin 전용. */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/admin/meeting-buildings")
@RequireAdmin
public class MeetingBuildingAdminController extends BaseController {

    private final BuildingAdminUseCase buildingAdminUseCase;

    public MeetingBuildingAdminController(BuildingAdminUseCase buildingAdminUseCase) {
        this.buildingAdminUseCase = buildingAdminUseCase;
    }

    /** 건물 목록 조회(siteId로 필터 가능). */
    @GetMapping
    public ApiResponse<List<BuildingResponse>> getBuildings(
            @CurrentUser AuthenticatedUser admin, @RequestParam(required = false) UUID siteId) {
        return ok(
                buildingAdminUseCase.list(siteId, admin.organizationId()).stream()
                        .map(BuildingResponse::from)
                        .toList());
    }

    /** 건물 등록. */
    @PostMapping
    public ResponseEntity<ApiResponse<BuildingResponse>> createBuilding(
            @CurrentUser AuthenticatedUser admin,
            @Valid @RequestBody BuildingRequest request) {
        return created(
                BuildingResponse.from(
                        buildingAdminUseCase.create(
                                request.siteId(), request.name(), admin.organizationId())));
    }

    /** 건물 수정. */
    @PatchMapping("/{buildingId}")
    public ApiResponse<BuildingResponse> updateBuilding(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID buildingId,
            @Valid @RequestBody BuildingRequest request) {
        return ok(
                BuildingResponse.from(
                        buildingAdminUseCase.update(
                                buildingId,
                                request.siteId(),
                                request.name(),
                                admin.organizationId())));
    }

    /** 건물 삭제(하위 회의실이 있으면 차단). */
    @DeleteMapping("/{buildingId}")
    public ApiResponse<Void> deleteBuilding(
            @CurrentUser AuthenticatedUser admin, @PathVariable UUID buildingId) {
        buildingAdminUseCase.delete(buildingId, admin.organizationId());
        return ok();
    }
}
