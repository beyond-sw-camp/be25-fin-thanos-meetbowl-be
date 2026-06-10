package com.meetbowl.infrastructure.persistence.community;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA 전용 repository다. application/domain 계층에서는 이 타입을 직접 참조하지 않는다. */
public interface SpringDataCommunityAliasRepository
        extends JpaRepository<CommunityAliasEntity, UUID> {

    Optional<CommunityAliasEntity> findByUserId(UUID userId);
}