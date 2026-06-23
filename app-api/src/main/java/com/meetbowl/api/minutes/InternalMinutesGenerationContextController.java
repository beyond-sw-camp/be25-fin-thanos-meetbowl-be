package com.meetbowl.api.minutes;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiHeaders;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.RequireSystem;
import com.meetbowl.application.minutes.GetMinutesGenerationContextUseCase;
import com.meetbowl.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/** AI 서버가 회의록 생성 입력을 조회하는 내부 시스템 전용 API다. */
@RequireSystem
@RestController
@SecurityRequirement(name = ApiHeaders.INTERNAL_TOKEN)
@RequestMapping(ApiPaths.API_V1 + "/internal/meetings/{meetingId}/minutes-generation-context")
public class InternalMinutesGenerationContextController extends BaseController {

    private final GetMinutesGenerationContextUseCase getContextUseCase;

    public InternalMinutesGenerationContextController(
            GetMinutesGenerationContextUseCase getContextUseCase) {
        this.getContextUseCase = getContextUseCase;
    }

    @GetMapping
    public ApiResponse<MinutesGenerationContextResponse> get(@PathVariable UUID meetingId) {
        return ok(MinutesGenerationContextResponse.from(getContextUseCase.execute(meetingId)));
    }
}
