package com.meetbowl.infrastructure.persistence.community;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.community.CommunityAlias;
import com.meetbowl.domain.community.CommunityAliasRepositoryPort;

/** CommunityAlias domain port를 JPA로 구현하는 adapter다. Entity ↔ Domain 변환은 이 경계에서만 수행한다. */
@Repository
public class JpaCommunityAliasRepositoryAdapter implements CommunityAliasRepositoryPort {

    private final SpringDataCommunityAliasRepository springDataCommunityAliasRepository;

    public JpaCommunityAliasRepositoryAdapter(
            SpringDataCommunityAliasRepository springDataCommunityAliasRepository) {
        this.springDataCommunityAliasRepository = springDataCommunityAliasRepository;
    }

    @Override
    public CommunityAlias save(CommunityAlias alias) {
        return springDataCommunityAliasRepository.save(CommunityAliasEntity.from(alias)).toDomain();
    }

    @Override
    public Optional<CommunityAlias> findByUserId(UUID userId) {
        return springDataCommunityAliasRepository
                .findByUserId(userId)
                .map(CommunityAliasEntity::toDomain);
    }

    @Override
    public List<CommunityAlias> findByUserIdIn(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return springDataCommunityAliasRepository.findByUserIdIn(userIds).stream()
                .map(CommunityAliasEntity::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return springDataCommunityAliasRepository.count();
    }
}
