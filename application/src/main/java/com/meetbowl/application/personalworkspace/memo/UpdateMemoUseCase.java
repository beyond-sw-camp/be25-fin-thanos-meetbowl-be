package com.meetbowl.application.personalworkspace.memo;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.document.DocumentIndexRequestedEvent;
import com.meetbowl.domain.document.DocumentIndexRequestedEventPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemo;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemoRepositoryPort;

/** 소유자 기준 조회로 본인 메모만 수정 대상으로 제한한다. */
@Service
public class UpdateMemoUseCase {

    private static final String PERSONAL_MEMO_DOCUMENT_TYPE = "PERSONAL_MEMO";

    private final PersonalWorkspaceMemoRepositoryPort memoRepositoryPort;
    private final DocumentIndexRequestedEventPort documentIndexRequestedEventPort;

    public UpdateMemoUseCase(
            PersonalWorkspaceMemoRepositoryPort memoRepositoryPort,
            DocumentIndexRequestedEventPort documentIndexRequestedEventPort) {
        this.memoRepositoryPort = memoRepositoryPort;
        this.documentIndexRequestedEventPort = documentIndexRequestedEventPort;
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

        PersonalWorkspaceMemo saved = memoRepositoryPort.save(updated);

        // 수정된 내용으로 재색인한다. 같은 documentId면 AI가 교체 upsert하므로 중복 없이 최신 본문이 반영된다.
        // organization은 선택값이라 조직 미소속 사용자도 그대로 발행한다(검색은 소유자 기준이라 조직 없이도 본인 검색에 잡힘).
        documentIndexRequestedEventPort.publish(
                new DocumentIndexRequestedEvent(
                        saved.id(),
                        PERSONAL_MEMO_DOCUMENT_TYPE,
                        command.organizationId(),
                        saved.ownerUserId(),
                        saved.title(),
                        saved.content(),
                        List.of(saved.ownerUserId()),
                        List.of(),
                        List.of()));

        return MemoResult.from(saved);
    }
}
