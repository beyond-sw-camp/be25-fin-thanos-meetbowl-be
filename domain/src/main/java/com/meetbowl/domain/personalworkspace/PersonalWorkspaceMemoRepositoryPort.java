package com.meetbowl.domain.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalWorkspaceMemoRepositoryPort {

    PersonalWorkspaceMemo save(PersonalWorkspaceMemo memo);

    Optional<PersonalWorkspaceMemo> findById(UUID memoId);

    List<PersonalWorkspaceMemo> findByOwnerUserId(UUID ownerUserId);

    void deleteById(UUID memoId);
}
