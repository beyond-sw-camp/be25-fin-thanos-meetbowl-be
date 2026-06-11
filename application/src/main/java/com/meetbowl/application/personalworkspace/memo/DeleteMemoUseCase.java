package com.meetbowl.application.personalworkspace.memo;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemoRepositoryPort;

/** 소유자 본인 메모만 삭제 대상으로 제한한다. */
@Service
public class DeleteMemoUseCase {

    private final PersonalWorkspaceMemoRepositoryPort memoRepositoryPort;

    public DeleteMemoUseCase(PersonalWorkspaceMemoRepositoryPort memoRepositoryPort) {
        this.memoRepositoryPort = memoRepositoryPort;
    }

    @Transactional
    public void execute(UUID userId, UUID memoId) {
        boolean deleted = memoRepositoryPort.deleteByIdAndOwnerUserId(memoId, userId);
        if (!deleted) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "삭제할 메모를 찾을 수 없습니다.");
        }
    }
}
