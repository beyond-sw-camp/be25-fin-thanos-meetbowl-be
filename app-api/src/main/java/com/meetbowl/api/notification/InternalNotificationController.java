package com.meetbowl.api.notification;

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
import com.meetbowl.api.notification.dto.InternalNotificationSendRequest;
import com.meetbowl.api.notification.dto.NotificationResponse;
import com.meetbowl.application.notification.DispatchNotificationUseCase;
import com.meetbowl.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * 시스템 내부 알림 발송 전용 Gateway Controller다.
 *
 * <p>회의 리마인더/수정/취소, 회의록 검토 요청·지연 같은 내부 흐름이 X-Internal-Token으로 호출한다. 사용자 알림 API와 경로/권한을 분리해, 화면 사용자가
 * 시스템 발송 경로를 직접 호출하지 못하게 한다. 발송 처리와 실시간 전달은 UseCase가 담당한다.
 */
@RestController
@RequireSystem
@SecurityRequirement(name = ApiHeaders.INTERNAL_TOKEN)
@RequestMapping(ApiPaths.API_V1 + "/internal/notifications")
public class InternalNotificationController extends BaseController {

    private final DispatchNotificationUseCase dispatchNotificationUseCase;

    public InternalNotificationController(
            DispatchNotificationUseCase dispatchNotificationUseCase) {
        this.dispatchNotificationUseCase = dispatchNotificationUseCase;
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<NotificationResponse>> send(
            @Valid @RequestBody InternalNotificationSendRequest request) {
        // 신뢰된 내부 입력을 application 계약으로 변환만 하고, 새 알림이 생성되므로 201로 응답한다.
        return created(
                NotificationResponse.from(
                        dispatchNotificationUseCase.execute(request.toCommand())));
    }
}