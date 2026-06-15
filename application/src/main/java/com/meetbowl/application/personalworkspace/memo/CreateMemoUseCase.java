package com.meetbowl.application.personalworkspace.memo;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.document.DocumentIndexRequestedEvent;
import com.meetbowl.domain.document.DocumentIndexRequestedEventPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemo;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemoRepositoryPort;

/** 현재 사용자의 개인 메모를 생성한다. 소유자는 인증 사용자에서 채워 다른 사용자 소유로 만들 수 없게 한다. */
@Service
public class CreateMemoUseCase {

    private static final String PERSONAL_MEMO_DOCUMENT_TYPE = "PERSONAL_MEMO";

    private final PersonalWorkspaceMemoRepositoryPort memoRepositoryPort;
    private final DocumentIndexRequestedEventPort documentIndexRequestedEventPort;

    public CreateMemoUseCase(
            PersonalWorkspaceMemoRepositoryPort memoRepositoryPort,
            DocumentIndexRequestedEventPort documentIndexRequestedEventPort) {
        this.memoRepositoryPort = memoRepositoryPort;
        this.documentIndexRequestedEventPort = documentIndexRequestedEventPort;
    }

    @Transactional
    public MemoResult execute(CreateMemoCommand command) {
        PersonalWorkspaceMemo memo =
                PersonalWorkspaceMemo.create(
                        command.ownerUserId(), command.title(), command.content(), Instant.now());

        PersonalWorkspaceMemo saved = memoRepositoryPort.save(memo);

        // 저장 성공 직후 AI 검색용 색인 이벤트를 발행한다. 개인 메모는 본인만 열람 가능하므로 접근 범위를 소유자로 제한한다.
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
