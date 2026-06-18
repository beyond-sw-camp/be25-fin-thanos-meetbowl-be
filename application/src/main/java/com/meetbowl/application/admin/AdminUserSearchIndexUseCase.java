package com.meetbowl.application.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.user.UserSearchIndexPort;

@Service
public class AdminUserSearchIndexUseCase {

    private final UserSearchIndexPort userSearchIndexPort;

    public AdminUserSearchIndexUseCase(UserSearchIndexPort userSearchIndexPort) {
        this.userSearchIndexPort = userSearchIndexPort;
    }

    @Transactional(readOnly = true)
    public ReindexResult reindexAll() {
        UserSearchIndexPort.ReindexResult result = userSearchIndexPort.reindexAll();
        return new ReindexResult(result.processedCount(), result.failedCount());
    }

    public record ReindexResult(long processedCount, long failedCount) {}
}
