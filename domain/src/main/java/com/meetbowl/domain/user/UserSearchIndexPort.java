package com.meetbowl.domain.user;

import java.util.UUID;

public interface UserSearchIndexPort {

    void indexUser(UUID userId);

    ReindexResult reindexAll();

    void reindexByAffiliateId(UUID affiliateId);

    void reindexByDepartmentId(UUID departmentId);

    void reindexByTeamId(UUID teamId);

    void reindexByPositionId(UUID positionId);

    record ReindexResult(long processedCount, long failedCount) {}
}
