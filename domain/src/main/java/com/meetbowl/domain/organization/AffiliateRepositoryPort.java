package com.meetbowl.domain.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AffiliateRepositoryPort {

    Affiliate save(Affiliate organization);

    Optional<Affiliate> findById(UUID organizationId);

    List<Affiliate> findAll();

    List<Affiliate> findAllByIds(Collection<UUID> organizationIds);

    boolean existsByName(String name);

    boolean existsByCode(String code);

    boolean existsByNameAndIdNot(String name, UUID affiliateId);

    boolean existsByCodeAndIdNot(String code, UUID affiliateId);
}
