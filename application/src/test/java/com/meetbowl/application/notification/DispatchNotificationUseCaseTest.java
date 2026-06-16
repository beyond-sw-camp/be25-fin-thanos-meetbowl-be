package com.meetbowl.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.notification.Notification;
import com.meetbowl.domain.notification.NotificationRepositoryPort;
import com.meetbowl.domain.notification.NotificationResourceType;
import com.meetbowl.domain.notification.NotificationType;

/**
 * 내부 알림 발송 UseCase의 저장·실시간 전달·입력 검증을 확인한다.
 *
 * <p>활성 트랜잭션이 없는 단위 테스트에서는 저장 직후 즉시 실시간 전달(publish)이 일어난다(운영에서는 커밋 이후로 미뤄진다).
 */
class DispatchNotificationUseCaseTest {

    private final NotificationRepositoryPort repository = mock(NotificationRepositoryPort.class);
    private final NotificationRealtimePort realtimePort = mock(NotificationRealtimePort.class);
    private final DispatchNotificationUseCase useCase =
            new DispatchNotificationUseCase(repository, realtimePort);

    @Test
    void savesNotificationAndPublishesToRecipient() {
        UUID recipientId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID savedId = UUID.randomUUID();
        when(repository.save(any()))
                .thenAnswer(
                        invocation -> {
                            Notification input = invocation.getArgument(0);
                            // 저장 시 id/생성 시각이 채워진 상태를 재현한다.
                            return Notification.of(
                                    savedId,
                                    input.recipientUserId(),
                                    input.type(),
                                    input.title(),
                                    input.content(),
                                    input.resourceType(),
                                    input.resourceId(),
                                    input.readAt(),
                                    Instant.parse("2099-01-01T00:00:00Z"));
                        });

        NotificationResult result =
                useCase.execute(
                        new DispatchNotificationCommand(
                                recipientId,
                                "MINUTES_REVIEW_REQUEST",
                                "회의록 검토 요청",
                                "회의록 검토를 요청합니다.",
                                "MEETING_MINUTES",
                                resourceId));

        assertThat(result.id()).isEqualTo(savedId);
        assertThat(result.type()).isEqualTo(NotificationType.MINUTES_REVIEW_REQUEST.name());
        assertThat(result.resourceType()).isEqualTo(NotificationResourceType.MEETING_MINUTES.name());
        assertThat(result.read()).isFalse();
        verify(realtimePort).publish(eq(recipientId), any(NotificationResult.class));
    }

    @Test
    void rejectsUnknownType() {
        assertThatThrownBy(
                        () ->
                                useCase.execute(
                                        new DispatchNotificationCommand(
                                                UUID.randomUUID(),
                                                "NOT_A_REAL_TYPE",
                                                "제목",
                                                "내용",
                                                null,
                                                null)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
        verify(repository, never()).save(any());
        verify(realtimePort, never()).publish(any(), any());
    }
}
