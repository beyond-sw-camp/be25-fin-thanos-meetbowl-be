package com.meetbowl.api.health;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.common.response.ApiResponse;

/** 배포와 로컬 실행 확인을 위한 최소 헬스 체크 엔드포인트다. */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/health")
public class HealthController extends BaseController {

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ok(new HealthResponse("UP", Instant.now()));
    }
}
