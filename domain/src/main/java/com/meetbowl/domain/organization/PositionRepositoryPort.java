package com.meetbowl.domain.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionRepositoryPort {

    Position save(Position position);

    void deleteById(UUID positionId);

    Optional<Position> findById(UUID positionId);

    // 관리자 기준정보 화면 목록 조회용 전체 데이터 조회
    List<Position> findAll();

    List<Position> findAllForExcelExport();

    List<Position> findAllByAffiliateId(UUID affiliateId);

    List<Position> findAllByIds(Collection<UUID> positionIds);

    boolean existsByAffiliateIdAndName(UUID affiliateId, String name);

    boolean existsByCode(String code);

    // 수정 시에는 자기 자신을 제외하고 같은 계열사 안의 중복 여부를 판단해야 한다.
    boolean existsByAffiliateIdAndNameAndIdNot(UUID affiliateId, String name, UUID positionId);

    boolean existsByCodeAndIdNot(String code, UUID positionId);

    boolean existsByAffiliateIdAndSortOrder(UUID affiliateId, Integer sortOrder);

    boolean existsByAffiliateIdAndSortOrderAndIdNot(
            UUID affiliateId, Integer sortOrder, UUID positionId);
}
