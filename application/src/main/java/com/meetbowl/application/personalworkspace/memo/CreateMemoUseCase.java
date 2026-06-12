package com.meetbowl.application.personalworkspace.memo;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemo;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemoRepositoryPort;

/** 현재 사용자의 개인 메모를 생성한다. 소유자는 인증 사용자에서 채워 다른 사용자 소유로 만들 수 없게 한다. */
@Service
public class CreateMemoUseCase {

    private final PersonalWorkspaceMemoRepositoryPort memoRepositoryPort;

    public CreateMemoUseCase(PersonalWorkspaceMemoRepositoryPort memoRepositoryPort) {
        this.memoRepositoryPort = memoRepositoryPort;
    }

    @Transactional
    public MemoResult execute(CreateMemoCommand command) {
        PersonalWorkspaceMemo memo =
                PersonalWorkspaceMemo.create(
                        command.ownerUserId(), command.title(), command.content(), Instant.now());

        return MemoResult.from(memoRepositoryPort.save(memo));
    }
}
