package com.meetbowl.application.personalworkspace.memo;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.document.DocumentIndexRemovedEvent;
import com.meetbowl.domain.document.DocumentIndexRemovedEventPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemoRepositoryPort;

/** 소유자 본인 메모만 삭제 대상으로 제한한다. 삭제된 메모는 챗봇 검색에서도 빠지도록 색인 제거 이벤트를 발행한다. */
@Service
public class DeleteMemoUseCase {

    private final PersonalWorkspaceMemoRepositoryPort memoRepositoryPort;
    private final DocumentIndexRemovedEventPort documentIndexRemovedEventPort;

    public DeleteMemoUseCase(
            PersonalWorkspaceMemoRepositoryPort memoRepositoryPort,
            DocumentIndexRemovedEventPort documentIndexRemovedEventPort) {
        this.memoRepositoryPort = memoRepositoryPort;
        this.documentIndexRemovedEventPort = documentIndexRemovedEventPort;
    }

    @Transactional
    public void execute(UUID userId, UUID memoId) {
        boolean deleted = memoRepositoryPort.deleteByIdAndOwnerUserId(memoId, userId);
        if (!deleted) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "삭제할 메모를 찾을 수 없습니다.");
        }
        // 실제로 삭제된 경우에만 색인 제거를 발행한다. documentId는 색인 시 사용한 memoId와 동일하다.
        documentIndexRemovedEventPort.publish(new DocumentIndexRemovedEvent(memoId));
    }
}
