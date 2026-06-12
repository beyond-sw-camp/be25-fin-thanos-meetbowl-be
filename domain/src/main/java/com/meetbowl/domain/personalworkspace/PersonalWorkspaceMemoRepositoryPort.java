package com.meetbowl.domain.personalworkspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 개인 메모 도메인이 저장 기술을 모르고 영속화를 요청하기 위한 경계다. 조회·삭제를 소유자 ID와 함께 받아 소유권 경계를 강제한다. */
public interface PersonalWorkspaceMemoRepositoryPort {

    PersonalWorkspaceMemo save(PersonalWorkspaceMemo memo);

    Optional<PersonalWorkspaceMemo> findByIdAndOwnerUserId(UUID memoId, UUID ownerUserId);

    List<PersonalWorkspaceMemo> findByOwnerUserId(UUID ownerUserId);

    boolean deleteByIdAndOwnerUserId(UUID memoId, UUID ownerUserId);
}
