package com.meetbowl.api.sampletask;

import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.application.sampletask.CreateSampleTaskCommand;
import com.meetbowl.application.sampletask.CreateSampleTaskUseCase;
import com.meetbowl.application.sampletask.SampleTaskResult;
import com.meetbowl.common.response.ApiResponse;

/** 계층별 변환 흐름을 보여주는 샘플 Controller다. sample 또는 sample-jpa 프로필에서만 노출되며 실제 API 명세 대상이 아니다. */
@Profile("sample | sample-jpa")
@RestController
@RequestMapping(ApiPaths.API_V1 + "/sample-tasks")
public class SampleTaskController extends BaseController {

    private final CreateSampleTaskUseCase createSampleTaskUseCase;

    public SampleTaskController(CreateSampleTaskUseCase createSampleTaskUseCase) {
        this.createSampleTaskUseCase = createSampleTaskUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SampleTaskResponse>> createSampleTask(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody CreateSampleTaskRequest request) {
        CreateSampleTaskCommand command =
                new CreateSampleTaskCommand(user.userId(), request.title());
        SampleTaskResult result = createSampleTaskUseCase.execute(command);
        return created(SampleTaskResponse.from(result));
    }
}
