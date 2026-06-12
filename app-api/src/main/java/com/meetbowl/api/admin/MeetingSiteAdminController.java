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
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.RequireAdmin;
import com.meetbowl.application.meetingroom.SiteAdminUseCase;
import com.meetbowl.common.response.ApiResponse;

/** 관리자 사이트(거점) 기준정보 관리 API다(F1, FR-095). Admin 전용. */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/admin/meeting-sites")
@RequireAdmin
public class MeetingSiteAdminController extends BaseController {

    private final SiteAdminUseCase siteAdminUseCase;

    public MeetingSiteAdminController(SiteAdminUseCase siteAdminUseCase) {
        this.siteAdminUseCase = siteAdminUseCase;
    }

    /** 사이트 목록 조회. */
    @GetMapping
    public ApiResponse<List<SiteResponse>> getSites() {
        return ok(siteAdminUseCase.list().stream().map(SiteResponse::from).toList());
    }

    /** 사이트 등록. */
    @PostMapping
    public ResponseEntity<ApiResponse<SiteResponse>> createSite(
            @Valid @RequestBody SiteRequest request) {
        return created(
                SiteResponse.from(siteAdminUseCase.create(request.name(), request.address())));
    }

    /** 사이트 수정. */
    @PatchMapping("/{siteId}")
    public ApiResponse<SiteResponse> updateSite(
            @PathVariable UUID siteId, @Valid @RequestBody SiteRequest request) {
        return ok(
                SiteResponse.from(
                        siteAdminUseCase.update(siteId, request.name(), request.address())));
    }

    /** 사이트 삭제(하위 건물이 있으면 차단). */
    @DeleteMapping("/{siteId}")
    public ApiResponse<Void> deleteSite(@PathVariable UUID siteId) {
        siteAdminUseCase.delete(siteId);
        return ok();
    }
}
