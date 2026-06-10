package com.meetbowl.application.personalworkspace.memo;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemo;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemoRepositoryPort;

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
