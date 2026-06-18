package com.meetbowl.domain.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AffiliateRepositoryPort {

    Affiliate save(Affiliate organization);

    Optional<Affiliate> findById(UUID organizationId);

    // 관리자 기준정보 화면 목록 조회용 전체 데이터 조회
    List<Affiliate> findAll();

    List<Affiliate> findAllForExcelExport();

    List<Affiliate> findAllByIds(Collection<UUID> organizationIds);

    boolean existsByName(String name);

    boolean existsByCode(String code);

    // 수정 시에는 자기 자신을 제외하고 중복 여부를 판단해야 한다.
    boolean existsByNameAndIdNot(String name, UUID affiliateId);

    boolean existsByCodeAndIdNot(String code, UUID affiliateId);
}
