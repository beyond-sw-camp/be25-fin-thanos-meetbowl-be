package com.meetbowl.application.personalworkspace.memo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemoRepositoryPort;

/** 개인 메모 수정/삭제의 소유자 조회 실패 분기를 검증한다. */
class MemoUseCaseTest {

    private final PersonalWorkspaceMemoRepositoryPort memoPort =
            Mockito.mock(PersonalWorkspaceMemoRepositoryPort.class);

    @Test
    void updateMemo_fail_when_not_found() {
        UpdateMemoUseCase useCase = new UpdateMemoUseCase(memoPort);
        UUID userId = UUID.randomUUID();
        UUID memoId = UUID.randomUUID();
        when(memoPort.findByIdAndOwnerUserId(memoId, userId)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(new UpdateMemoCommand(memoId, userId, "제목", "내용")));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, ex.errorCode());
        verify(memoPort, never()).save(any());
    }

    @Test
    void deleteMemo_fail_when_nothing_deleted() {
        DeleteMemoUseCase useCase = new DeleteMemoUseCase(memoPort);
        UUID userId = UUID.randomUUID();
        UUID memoId = UUID.randomUUID();
        when(memoPort.deleteByIdAndOwnerUserId(memoId, userId)).thenReturn(false);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> useCase.execute(userId, memoId));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, ex.errorCode());
    }
}
