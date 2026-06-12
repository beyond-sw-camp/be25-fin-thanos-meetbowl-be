package com.meetbowl.application.personalworkspace.memo;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemo;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemoRepositoryPort;

/** 소유자 기준 조회로 본인 메모만 수정 대상으로 제한한다. */
@Service
public class UpdateMemoUseCase {

    private final PersonalWorkspaceMemoRepositoryPort memoRepositoryPort;

    public UpdateMemoUseCase(PersonalWorkspaceMemoRepositoryPort memoRepositoryPort) {
        this.memoRepositoryPort = memoRepositoryPort;
    }

    @Transactional
    public MemoResult execute(UpdateMemoCommand command) {
        PersonalWorkspaceMemo memo =
                memoRepositoryPort
                        .findByIdAndOwnerUserId(command.memoId(), command.ownerUserId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "메모를 찾을 수 없습니다."));

        PersonalWorkspaceMemo updated =
                memo.update(command.title(), command.content(), Instant.now());

        return MemoResult.from(memoRepositoryPort.save(updated));
    }
}
