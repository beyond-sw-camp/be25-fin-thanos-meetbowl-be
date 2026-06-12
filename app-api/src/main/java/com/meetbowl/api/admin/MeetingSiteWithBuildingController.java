package com.meetbowl.api.admin;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.RequireAdmin;
import com.meetbowl.application.meetingroom.SiteBuildingRegisterUseCase;
import com.meetbowl.common.response.ApiResponse;

/**
 * 사이트+건물 동시 등록 API다(F1 보강). "사이트/건물 추가" 화면에서 한 폼으로 사이트와 건물을 함께 만든다.
 *
 * <p>개별 CRUD({@code /admin/meeting-sites}, {@code /admin/meeting-buildings})와 별개의 묶음 등록 동작이라 별도
 * 컨트롤러로 둔다. Admin 전용.
 */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/admin/meeting-sites-with-building")
@RequireAdmin
public class MeetingSiteWithBuildingController extends BaseController {

    private final SiteBuildingRegisterUseCase siteBuildingRegisterUseCase;

    public MeetingSiteWithBuildingController(
            SiteBuildingRegisterUseCase siteBuildingRegisterUseCase) {
        this.siteBuildingRegisterUseCase = siteBuildingRegisterUseCase;
    }

    /** 사이트와 건물을 한 트랜잭션으로 함께 등록한다. 둘 중 하나라도 실패하면 전체 롤백된다. */
    @PostMapping
    public ResponseEntity<ApiResponse<SiteWithBuildingResponse>> registerSiteWithBuilding(
            @Valid @RequestBody SiteWithBuildingRequest request) {
        return created(
                SiteWithBuildingResponse.from(
                        siteBuildingRegisterUseCase.execute(
                                request.siteName(), request.buildingName())));
    }
}