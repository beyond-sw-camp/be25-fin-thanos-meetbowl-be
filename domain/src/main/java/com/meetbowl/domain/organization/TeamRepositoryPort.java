package com.meetbowl.domain.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepositoryPort {

    Team save(Team team);

    Optional<Team> findById(UUID teamId);

    // 관리자 기준정보 화면 목록 조회용 전체 데이터 조회
    List<Team> findAll();

    List<Team> findAllForExcelExport();

    List<Team> findAllByIds(Collection<UUID> teamIds);

    // 팀명 중복 규칙은 같은 Department 내부 범위에서만 적용된다.
    boolean existsByDepartmentIdAndName(UUID departmentId, String name);

    boolean existsByDepartmentIdAndNameAndIdNot(UUID departmentId, String name, UUID teamId);
}
