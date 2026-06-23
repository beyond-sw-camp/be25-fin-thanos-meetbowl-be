package com.meetbowl.domain.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepositoryPort {

    Department save(Department department);

    void deleteById(UUID departmentId);

    Optional<Department> findById(UUID departmentId);

    // 관리자 기준정보 화면 목록 조회용 전체 데이터 조회
    List<Department> findAll();

    List<Department> findAllForExcelExport();

    List<Department> findAllByIds(Collection<UUID> departmentIds);

    // 부서명 중복 규칙은 전체가 아니라 같은 Affiliate 내부 범위에서만 적용된다.
    boolean existsByAffiliateIdAndName(UUID affiliateId, String name);

    boolean existsByAffiliateIdAndNameAndIdNot(UUID affiliateId, String name, UUID departmentId);
}
