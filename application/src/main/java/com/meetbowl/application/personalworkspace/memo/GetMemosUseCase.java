package com.meetbowl.application.personalworkspace.memo;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceMemoRepositoryPort;

/** 목록 정렬(최근 수정 순)은 영속성 어댑터가 책임지므로 UseCase는 정렬을 다루지 않는다. */
@Service
public class GetMemosUseCase {

    private final PersonalWorkspaceMemoRepositoryPort memoRepositoryPort;

    public GetMemosUseCase(PersonalWorkspaceMemoRepositoryPort memoRepositoryPort) {
        this.memoRepositoryPort = memoRepositoryPort;
    }

    @Transactional(readOnly = true)
    public List<MemoResult> execute(UUID userId) {
        return memoRepositoryPort.findByOwnerUserId(userId).stream().map(MemoResult::from).toList();
    }
}
