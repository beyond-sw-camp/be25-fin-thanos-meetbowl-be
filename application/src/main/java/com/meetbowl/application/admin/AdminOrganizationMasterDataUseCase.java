package com.meetbowl.application.admin;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.organization.Affiliate;
import com.meetbowl.domain.organization.AffiliateRepositoryPort;
import com.meetbowl.domain.organization.Department;
import com.meetbowl.domain.organization.DepartmentRepositoryPort;
import com.meetbowl.domain.organization.Position;
import com.meetbowl.domain.organization.PositionRepositoryPort;
import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.domain.organization.Team;
import com.meetbowl.domain.organization.TeamRepositoryPort;
import com.meetbowl.domain.user.UserSearchIndexPort;

@Service
public class AdminOrganizationMasterDataUseCase {

    // 관리자 화면에서는 수동 정렬값을 우선하고, 값이 같으면 이름/ID 순으로 안정적으로 보여준다.
    private static final Comparator<Affiliate> AFFILIATE_SORT =
            Comparator.comparing(Affiliate::sortOrder, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(Affiliate::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Affiliate::id);

    private static final Comparator<Department> DEPARTMENT_SORT =
            Comparator.comparing(Department::sortOrder, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(Department::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Department::id);

    private static final Comparator<Team> TEAM_SORT =
            Comparator.comparing(Team::sortOrder, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(Team::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Team::id);

    private static final Comparator<Position> POSITION_SORT =
            Comparator.comparing(Position::sortOrder, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(Position::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Position::id);

    private final AffiliateRepositoryPort affiliateRepositoryPort;
    private final DepartmentRepositoryPort departmentRepositoryPort;
    private final TeamRepositoryPort teamRepositoryPort;
    private final PositionRepositoryPort positionRepositoryPort;
    private final UserSearchIndexPort userSearchIndexPort;

    public AdminOrganizationMasterDataUseCase(
            AffiliateRepositoryPort affiliateRepositoryPort,
            DepartmentRepositoryPort departmentRepositoryPort,
            TeamRepositoryPort teamRepositoryPort,
            PositionRepositoryPort positionRepositoryPort,
            UserSearchIndexPort userSearchIndexPort) {
        this.affiliateRepositoryPort = affiliateRepositoryPort;
        this.departmentRepositoryPort = departmentRepositoryPort;
        this.teamRepositoryPort = teamRepositoryPort;
        this.positionRepositoryPort = positionRepositoryPort;
        this.userSearchIndexPort = userSearchIndexPort;
    }

    @Transactional(readOnly = true)
    public List<AffiliateResult> getAffiliates() {
        return affiliateRepositoryPort.findAll().stream()
                .sorted(AFFILIATE_SORT)
                .map(AdminOrganizationMasterDataUseCase::toAffiliateResult)
                .toList();
    }

    @Transactional
    public AffiliateResult createAffiliate(CreateAffiliateCommand command) {
        String name = requiredText(command.name(), "Affiliate name is required.");
        String code = requiredText(command.code(), "Affiliate code is required.");
        // 계열사는 전역 기준정보이므로 이름/코드 모두 전체 범위에서 중복을 막는다.
        ensureAffiliateUnique(name, code, null);
        return toAffiliateResult(
                affiliateRepositoryPort.save(
                        new Affiliate(
                                UUID.randomUUID(),
                                name,
                                code,
                                parseStatus(command.status()),
                                command.sortOrder(),
                                Instant.now(),
                                Instant.now())));
    }

    @Transactional
    public AffiliateResult updateAffiliate(UpdateAffiliateCommand command) {
        Affiliate affiliate = loadAffiliate(command.affiliateId());
        String name = requiredText(command.name(), "Affiliate name is required.");
        String code = requiredText(command.code(), "Affiliate code is required.");
        ensureAffiliateUnique(name, code, affiliate.id());
        Affiliate saved =
                affiliateRepositoryPort.save(
                        new Affiliate(
                                affiliate.id(),
                                name,
                                code,
                                affiliate.status(),
                                command.sortOrder(),
                                affiliate.createdAt(),
                                Instant.now()));
        // 조직 표시명 변경은 사용자 검색 문서의 노출값에도 반영돼야 하므로 관련 문서를 함께 재색인한다.
        userSearchIndexPort.reindexByAffiliateId(saved.id());
        return toAffiliateResult(saved);
    }

    @Transactional
    public AffiliateResult updateAffiliateStatus(UpdateAffiliateStatusCommand command) {
        Affiliate affiliate = loadAffiliate(command.affiliateId());
        return toAffiliateResult(
                affiliateRepositoryPort.save(
                        new Affiliate(
                                affiliate.id(),
                                affiliate.name(),
                                affiliate.code(),
                                parseStatus(command.status()),
                                affiliate.sortOrder(),
                                affiliate.createdAt(),
                                Instant.now())));
    }

    @Transactional(readOnly = true)
    public List<DepartmentResult> getDepartments() {
        return departmentRepositoryPort.findAll().stream()
                .sorted(DEPARTMENT_SORT)
                .map(AdminOrganizationMasterDataUseCase::toDepartmentResult)
                .toList();
    }

    @Transactional
    public DepartmentResult createDepartment(CreateDepartmentCommand command) {
        String name = requiredText(command.name(), "Department name is required.");
        String code = requiredText(command.code(), "Department code is required.");
        // 부서는 Affiliate 하위 기준정보이므로 상위 Affiliate가 실제로 존재해야 한다.
        loadAffiliate(command.affiliateId());
        // 같은 Affiliate 안에서는 동일한 부서명을 허용하지 않는다.
        ensureDepartmentNameUnique(command.affiliateId(), name, null);
        return toDepartmentResult(
                departmentRepositoryPort.save(
                        new Department(
                                UUID.randomUUID(),
                                command.affiliateId(),
                                null,
                                name,
                                code,
                                parseStatus(command.status()),
                                command.sortOrder(),
                                Instant.now(),
                                Instant.now())));
    }

    @Transactional
    public DepartmentResult updateDepartment(UpdateDepartmentCommand command) {
        Department department = loadDepartment(command.departmentId());
        String name = requiredText(command.name(), "Department name is required.");
        String code = requiredText(command.code(), "Department code is required.");
        loadAffiliate(command.affiliateId());
        ensureDepartmentNameUnique(command.affiliateId(), name, department.id());
        Department saved =
                departmentRepositoryPort.save(
                        new Department(
                                department.id(),
                                command.affiliateId(),
                                department.parentDepartmentId(),
                                name,
                                code,
                                department.status(),
                                command.sortOrder(),
                                department.createdAt(),
                                Instant.now()));
        userSearchIndexPort.reindexByDepartmentId(saved.id());
        return toDepartmentResult(saved);
    }

    @Transactional
    public DepartmentResult updateDepartmentStatus(UpdateDepartmentStatusCommand command) {
        Department department = loadDepartment(command.departmentId());
        return toDepartmentResult(
                departmentRepositoryPort.save(
                        new Department(
                                department.id(),
                                department.affiliateId(),
                                department.parentDepartmentId(),
                                department.name(),
                                department.code(),
                                parseStatus(command.status()),
                                department.sortOrder(),
                                department.createdAt(),
                                Instant.now())));
    }

    @Transactional(readOnly = true)
    public List<TeamResult> getTeams() {
        return teamRepositoryPort.findAll().stream()
                .sorted(TEAM_SORT)
                .map(AdminOrganizationMasterDataUseCase::toTeamResult)
                .toList();
    }

    @Transactional
    public TeamResult createTeam(CreateTeamCommand command) {
        String name = requiredText(command.name(), "Team name is required.");
        String code = requiredText(command.code(), "Team code is required.");
        // 팀은 Department에 소속되므로 상위 Department 존재 여부를 먼저 확인한다.
        loadDepartment(command.departmentId());
        // 같은 Department 안에서는 동일한 팀명을 허용하지 않는다.
        ensureTeamNameUnique(command.departmentId(), name, null);
        return toTeamResult(
                teamRepositoryPort.save(
                        new Team(
                                UUID.randomUUID(),
                                command.departmentId(),
                                name,
                                code,
                                parseStatus(command.status()),
                                command.sortOrder(),
                                Instant.now(),
                                Instant.now())));
    }

    @Transactional
    public TeamResult updateTeam(UpdateTeamCommand command) {
        Team team = loadTeam(command.teamId());
        String name = requiredText(command.name(), "Team name is required.");
        String code = requiredText(command.code(), "Team code is required.");
        loadDepartment(command.departmentId());
        ensureTeamNameUnique(command.departmentId(), name, team.id());
        Team saved =
                teamRepositoryPort.save(
                        new Team(
                                team.id(),
                                command.departmentId(),
                                name,
                                code,
                                team.status(),
                                command.sortOrder(),
                                team.createdAt(),
                                Instant.now()));
        userSearchIndexPort.reindexByTeamId(saved.id());
        return toTeamResult(saved);
    }

    @Transactional
    public TeamResult updateTeamStatus(UpdateTeamStatusCommand command) {
        Team team = loadTeam(command.teamId());
        return toTeamResult(
                teamRepositoryPort.save(
                        new Team(
                                team.id(),
                                team.departmentId(),
                                team.name(),
                                team.code(),
                                parseStatus(command.status()),
                                team.sortOrder(),
                                team.createdAt(),
                                Instant.now())));
    }

    @Transactional(readOnly = true)
    public List<PositionResult> getPositions() {
        return positionRepositoryPort.findAll().stream()
                .sorted(POSITION_SORT)
                .map(AdminOrganizationMasterDataUseCase::toPositionResult)
                .toList();
    }

    @Transactional
    public PositionResult createPosition(CreatePositionCommand command) {
        String name = requiredText(command.name(), "Position name is required.");
        String code = requiredText(command.code(), "Position code is required.");
        // 직급은 독립 기준정보이므로 이름/코드를 전역으로 유니크하게 관리한다.
        ensurePositionUnique(name, code, null);
        return toPositionResult(
                positionRepositoryPort.save(
                        new Position(
                                UUID.randomUUID(),
                                name,
                                code,
                                parseStatus(command.status()),
                                command.sortOrder(),
                                Instant.now(),
                                Instant.now())));
    }

    @Transactional
    public PositionResult updatePosition(UpdatePositionCommand command) {
        Position position = loadPosition(command.positionId());
        String name = requiredText(command.name(), "Position name is required.");
        String code = requiredText(command.code(), "Position code is required.");
        ensurePositionUnique(name, code, position.id());
        Position saved =
                positionRepositoryPort.save(
                        new Position(
                                position.id(),
                                name,
                                code,
                                position.status(),
                                command.sortOrder(),
                                position.createdAt(),
                                Instant.now()));
        userSearchIndexPort.reindexByPositionId(saved.id());
        return toPositionResult(saved);
    }

    @Transactional
    public PositionResult updatePositionStatus(UpdatePositionStatusCommand command) {
        Position position = loadPosition(command.positionId());
        return toPositionResult(
                positionRepositoryPort.save(
                        new Position(
                                position.id(),
                                position.name(),
                                position.code(),
                                parseStatus(command.status()),
                                position.sortOrder(),
                                position.createdAt(),
                                Instant.now())));
    }

    private void ensureAffiliateUnique(String name, String code, UUID affiliateId) {
        boolean duplicatedName =
                affiliateId == null
                        ? affiliateRepositoryPort.existsByName(name)
                        : affiliateRepositoryPort.existsByNameAndIdNot(name, affiliateId);
        if (duplicatedName) {
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT, "Affiliate name already exists.");
        }
        boolean duplicatedCode =
                affiliateId == null
                        ? affiliateRepositoryPort.existsByCode(code)
                        : affiliateRepositoryPort.existsByCodeAndIdNot(code, affiliateId);
        if (duplicatedCode) {
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT, "Affiliate code already exists.");
        }
    }

    private void ensureDepartmentNameUnique(UUID affiliateId, String name, UUID departmentId) {
        boolean duplicated =
                departmentId == null
                        ? departmentRepositoryPort.existsByAffiliateIdAndName(affiliateId, name)
                        : departmentRepositoryPort.existsByAffiliateIdAndNameAndIdNot(
                                affiliateId, name, departmentId);
        if (duplicated) {
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT, "Department name already exists in the affiliate.");
        }
    }

    private void ensureTeamNameUnique(UUID departmentId, String name, UUID teamId) {
        boolean duplicated =
                teamId == null
                        ? teamRepositoryPort.existsByDepartmentIdAndName(departmentId, name)
                        : teamRepositoryPort.existsByDepartmentIdAndNameAndIdNot(
                                departmentId, name, teamId);
        if (duplicated) {
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT, "Team name already exists in the department.");
        }
    }

    private void ensurePositionUnique(String name, String code, UUID positionId) {
        boolean duplicatedName =
                positionId == null
                        ? positionRepositoryPort.existsByName(name)
                        : positionRepositoryPort.existsByNameAndIdNot(name, positionId);
        if (duplicatedName) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "Position name already exists.");
        }
        boolean duplicatedCode =
                positionId == null
                        ? positionRepositoryPort.existsByCode(code)
                        : positionRepositoryPort.existsByCodeAndIdNot(code, positionId);
        if (duplicatedCode) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "Position code already exists.");
        }
    }

    private Affiliate loadAffiliate(UUID affiliateId) {
        if (affiliateId == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "Affiliate ID is required.");
        }
        // 생성/수정 요청에서 상위 조직 ID를 넘겼을 때 조용히 null 처리하지 않고 즉시 실패시킨다.
        return affiliateRepositoryPort
                .findById(affiliateId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.COMMON_NOT_FOUND, "Affiliate not found."));
    }

    private Department loadDepartment(UUID departmentId) {
        if (departmentId == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "Department ID is required.");
        }
        return departmentRepositoryPort
                .findById(departmentId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.COMMON_NOT_FOUND, "Department not found."));
    }

    private Team loadTeam(UUID teamId) {
        if (teamId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "Team ID is required.");
        }
        return teamRepositoryPort
                .findById(teamId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "Team not found."));
    }

    private Position loadPosition(UUID positionId) {
        if (positionId == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "Position ID is required.");
        }
        return positionRepositoryPort
                .findById(positionId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.COMMON_NOT_FOUND, "Position not found."));
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
        }
        return value.trim();
    }

    private ReferenceStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "Reference status is required.");
        }
        try {
            return ReferenceStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "Unsupported reference status.");
        }
    }

    private static AffiliateResult toAffiliateResult(Affiliate affiliate) {
        return new AffiliateResult(
                affiliate.id(),
                affiliate.name(),
                affiliate.code(),
                affiliate.status().name(),
                affiliate.sortOrder(),
                affiliate.createdAt(),
                affiliate.updatedAt());
    }

    private static DepartmentResult toDepartmentResult(Department department) {
        return new DepartmentResult(
                department.id(),
                department.affiliateId(),
                department.name(),
                department.code(),
                department.status().name(),
                department.sortOrder(),
                department.createdAt(),
                department.updatedAt());
    }

    private static TeamResult toTeamResult(Team team) {
        return new TeamResult(
                team.id(),
                team.departmentId(),
                team.name(),
                team.code(),
                team.status().name(),
                team.sortOrder(),
                team.createdAt(),
                team.updatedAt());
    }

    private static PositionResult toPositionResult(Position position) {
        return new PositionResult(
                position.id(),
                position.name(),
                position.code(),
                position.status().name(),
                position.sortOrder(),
                position.createdAt(),
                position.updatedAt());
    }

    public record CreateAffiliateCommand(
            String name, String code, String status, Integer sortOrder) {}

    public record UpdateAffiliateCommand(
            UUID affiliateId, String name, String code, Integer sortOrder) {}

    public record UpdateAffiliateStatusCommand(UUID affiliateId, String status) {}

    public record CreateDepartmentCommand(
            UUID affiliateId, String name, String code, String status, Integer sortOrder) {}

    public record UpdateDepartmentCommand(
            UUID departmentId, UUID affiliateId, String name, String code, Integer sortOrder) {}

    public record UpdateDepartmentStatusCommand(UUID departmentId, String status) {}

    public record CreateTeamCommand(
            UUID departmentId, String name, String code, String status, Integer sortOrder) {}

    public record UpdateTeamCommand(
            UUID teamId, UUID departmentId, String name, String code, Integer sortOrder) {}

    public record UpdateTeamStatusCommand(UUID teamId, String status) {}

    public record CreatePositionCommand(
            String name, String code, String status, Integer sortOrder) {}

    public record UpdatePositionCommand(
            UUID positionId, String name, String code, Integer sortOrder) {}

    public record UpdatePositionStatusCommand(UUID positionId, String status) {}

    public record AffiliateResult(
            UUID affiliateId,
            String name,
            String code,
            String status,
            Integer sortOrder,
            Instant createdAt,
            Instant updatedAt) {}

    public record DepartmentResult(
            UUID departmentId,
            UUID affiliateId,
            String name,
            String code,
            String status,
            Integer sortOrder,
            Instant createdAt,
            Instant updatedAt) {}

    public record TeamResult(
            UUID teamId,
            UUID departmentId,
            String name,
            String code,
            String status,
            Integer sortOrder,
            Instant createdAt,
            Instant updatedAt) {}

    public record PositionResult(
            UUID positionId,
            String name,
            String code,
            String status,
            Integer sortOrder,
            Instant createdAt,
            Instant updatedAt) {}
}
