package com.meetbowl.domain.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalWorkspaceMemoRepositoryPort {

    PersonalWorkspaceMemo save(PersonalWorkspaceMemo memo);

    Optional<PersonalWorkspaceMemo> findByIdAndOwnerUserId(UUID memoId, UUID ownerUserId);

    List<PersonalWorkspaceMemo> findByOwnerUserId(UUID ownerUserId);

    boolean deleteByIdAndOwnerUserId(UUID memoId, UUID ownerUserId);
}
