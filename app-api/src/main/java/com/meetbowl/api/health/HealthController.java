package com.meetbowl.api.health;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.common.response.ApiResponse;

/** 인증 설정이나 업무 의존성에 막히지 않고 배포 시스템이 애플리케이션 생존 여부를 확인할 수 있게 둔다. */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/health")
public class HealthController extends BaseController {

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ok(new HealthResponse("UP", Instant.now()));
    }
}
