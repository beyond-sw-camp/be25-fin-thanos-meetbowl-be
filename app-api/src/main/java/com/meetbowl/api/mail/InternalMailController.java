package com.meetbowl.api.mail;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiHeaders;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.RequireSystem;
import com.meetbowl.api.mail.dto.InternalMailSendRequest;
import com.meetbowl.api.mail.dto.SendMailResponse;
import com.meetbowl.application.mail.DispatchInternalMailUseCase;
import com.meetbowl.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * 시스템 내부 메일 발송 전용 Gateway Controller다.
 *
 * <p>회의록 공유 등 내부 흐름이 X-Internal-Token으로 호출한다. 사용자 메일 API와 경로/권한을 분리해, 화면 사용자가 시스템 발송 경로를 직접 호출하지
 * 못하게 한다. 발송 처리와 결과 이벤트 발행은 UseCase가 담당한다.
 */
@RestController
@RequireSystem
@SecurityRequirement(name = ApiHeaders.INTERNAL_TOKEN)
@RequestMapping(ApiPaths.API_V1 + "/internal/mails")
public class InternalMailController extends BaseController {

    private final DispatchInternalMailUseCase dispatchInternalMailUseCase;

    public InternalMailController(DispatchInternalMailUseCase dispatchInternalMailUseCase) {
        this.dispatchInternalMailUseCase = dispatchInternalMailUseCase;
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SendMailResponse>> send(
            @Valid @RequestBody InternalMailSendRequest request) {
        return created(
                SendMailResponse.from(dispatchInternalMailUseCase.execute(request.toCommand())));
    }
}
