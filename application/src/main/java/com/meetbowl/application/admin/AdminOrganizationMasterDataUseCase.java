package com.meetbowl.application.admin;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.application.user.UserSearchReindexRequestDispatcher;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.admin.AuditResult;
import com.meetbowl.domain.organization.Affiliate;
import com.meetbowl.domain.organization.AffiliateRepositoryPort;
import com.meetbowl.domain.organization.Department;
import com.meetbowl.domain.organization.DepartmentRepositoryPort;
import com.meetbowl.domain.organization.Position;
import com.meetbowl.domain.organization.PositionRepositoryPort;
import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.domain.organization.Team;
import com.meetbowl.domain.organization.TeamRepositoryPort;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserSearchReindexRequestedEvent;

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
    private final UserRepositoryPort userRepositoryPort;
    private final AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    private final ObjectMapper objectMapper;
    private final UserSearchReindexRequestDispatcher userSearchReindexRequestDispatcher;

    public AdminOrganizationMasterDataUseCase(
            AffiliateRepositoryPort affiliateRepositoryPort,
            DepartmentRepositoryPort departmentRepositoryPort,
            TeamRepositoryPort teamRepositoryPort,
            PositionRepositoryPort positionRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            AdminAuditLogRepositoryPort adminAuditLogRepositoryPort,
            ObjectMapper objectMapper,
            UserSearchReindexRequestDispatcher userSearchReindexRequestDispatcher) {
        this.affiliateRepositoryPort = affiliateRepositoryPort;
        this.departmentRepositoryPort = departmentRepositoryPort;
        this.teamRepositoryPort = teamRepositoryPort;
        this.positionRepositoryPort = positionRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.adminAuditLogRepositoryPort = adminAuditLogRepositoryPort;
        this.objectMapper = objectMapper;
        this.userSearchReindexRequestDispatcher = userSearchReindexRequestDispatcher;
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
        if (isAffiliateSearchDocumentChanged(affiliate, saved)) {
            publishAffiliateReindex(saved.id(), command.adminId(), "AFFILIATE_UPDATED");
        }
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
        String code = nextDepartmentCode();
        // 부서는 Affiliate 하위 기준정보이므로 상위 Affiliate가 실제로 존재해야 한다.
        loadAffiliate(command.affiliateId());
        // 같은 Affiliate 안에서는 동일한 부서명을 허용하지 않는다.
        ensureDepartmentNameUnique(command.affiliateId(), name, null);
        ensureDepartmentSortOrderUnique(command.affiliateId(), command.sortOrder(), null);
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
        loadAffiliate(command.affiliateId());
        ensureDepartmentNameUnique(command.affiliateId(), name, department.id());
        ensureDepartmentSortOrderUnique(command.affiliateId(), command.sortOrder(), department.id());
        Department saved =
                departmentRepositoryPort.save(
                        new Department(
                                department.id(),
                                command.affiliateId(),
                                department.parentDepartmentId(),
                                name,
                                // 수정 시 code를 바꾸면 기존 식별/엑셀 참조 흐름이 흔들릴 수 있어 서버가 기존 값을 고정한다.
                                department.code(),
                                department.status(),
                                command.sortOrder(),
                                department.createdAt(),
                                Instant.now()));
        if (isDepartmentSearchDocumentChanged(department, saved)) {
            publishDepartmentReindex(saved.id(), command.adminId(), "DEPARTMENT_UPDATED");
        }
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

    @Transactional
    public void deleteDepartment(DeleteDepartmentCommand command) {
        Department department =
                departmentRepositoryPort
                        .findById(command.departmentId())
                        .orElseThrow(
                                () -> {
                                    saveDeleteFailureAudit(
                                            command.adminId(),
                                            command.adminName(),
                                            command.ipAddress(),
                                            command.userAgent(),
                                            "DEPARTMENT",
                                            command.departmentId(),
                                            null,
                                            "삭제할 부서를 찾을 수 없습니다.");
                                    return new BusinessException(
                                            ErrorCode.COMMON_NOT_FOUND, "Department not found.");
                                });
        String beforeValue = snapshot(toDepartmentResult(department));

        // 부서 삭제 전에는 하위 팀과 소속 회원을 먼저 막아야 조직 트리와 회원 참조가 동시에 깨지지 않는다.
        if (!teamRepositoryPort.findAllByDepartmentId(department.id()).isEmpty()) {
            saveDeleteFailureAudit(
                    command.adminId(),
                    command.adminName(),
                    command.ipAddress(),
                    command.userAgent(),
                    "DEPARTMENT",
                    department.id(),
                    beforeValue,
                    "소속 팀이 있어 부서를 삭제할 수 없습니다.");
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "소속 팀이 있어 부서를 삭제할 수 없습니다.");
        }
        if (!userRepositoryPort.findAllByDepartmentId(department.id()).isEmpty()) {
            saveDeleteFailureAudit(
                    command.adminId(),
                    command.adminName(),
                    command.ipAddress(),
                    command.userAgent(),
                    "DEPARTMENT",
                    department.id(),
                    beforeValue,
                    "소속 회원이 있어 부서를 삭제할 수 없습니다.");
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "소속 회원이 있어 부서를 삭제할 수 없습니다.");
        }

        departmentRepositoryPort.deleteById(department.id());
        saveDeleteSuccessAudit(
                command.adminId(),
                command.adminName(),
                command.ipAddress(),
                command.userAgent(),
                "DEPARTMENT",
                department.id(),
                beforeValue);
        // 삭제는 회원 참조가 없을 때만 허용하므로 사용자 검색 문서를 추가로 갱신할 대상이 없다.
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
        String code = nextTeamCode();
        // 팀은 Department에 소속되므로 상위 Department 존재 여부를 먼저 확인한다.
        Department department = loadDepartment(command.departmentId());
        // 같은 Department 안에서는 동일한 팀명을 허용하지 않는다.
        ensureTeamNameUnique(command.departmentId(), name, null);
        // 팀 순서는 상위 부서와 무관하게 같은 계열사 전체 범위에서 유일해야 한다.
        ensureTeamSortOrderUnique(department.affiliateId(), command.sortOrder(), null);
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
        Department department = loadDepartment(command.departmentId());
        ensureTeamNameUnique(command.departmentId(), name, team.id());
        ensureTeamSortOrderUnique(department.affiliateId(), command.sortOrder(), team.id());
        Team saved =
                teamRepositoryPort.save(
                        new Team(
                                team.id(),
                                command.departmentId(),
                                name,
                                // 수정 시 code를 바꾸면 기존 식별/엑셀 참조 흐름이 흔들릴 수 있어 서버가 기존 값을 고정한다.
                                team.code(),
                                team.status(),
                                command.sortOrder(),
                                team.createdAt(),
                                Instant.now()));
        if (isTeamSearchDocumentChanged(team, saved)) {
            publishTeamReindex(saved.id(), command.adminId(), "TEAM_UPDATED");
        }
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

    @Transactional
    public void deleteTeam(DeleteTeamCommand command) {
        Team team =
                teamRepositoryPort
                        .findById(command.teamId())
                        .orElseThrow(
                                () -> {
                                    saveDeleteFailureAudit(
                                            command.adminId(),
                                            command.adminName(),
                                            command.ipAddress(),
                                            command.userAgent(),
                                            "TEAM",
                                            command.teamId(),
                                            null,
                                            "삭제할 팀을 찾을 수 없습니다.");
                                    return new BusinessException(
                                            ErrorCode.COMMON_NOT_FOUND, "Team not found.");
                                });
        String beforeValue = snapshot(toTeamResult(team));

        // 팀 삭제는 소속 회원이 없을 때만 허용해서 회원의 조직 참조가 dangling 상태가 되지 않게 막는다.
        if (!userRepositoryPort.findAllByTeamId(team.id()).isEmpty()) {
            saveDeleteFailureAudit(
                    command.adminId(),
                    command.adminName(),
                    command.ipAddress(),
                    command.userAgent(),
                    "TEAM",
                    team.id(),
                    beforeValue,
                    "소속 회원이 있어 팀을 삭제할 수 없습니다.");
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "소속 회원이 있어 팀을 삭제할 수 없습니다.");
        }

        teamRepositoryPort.deleteById(team.id());
        saveDeleteSuccessAudit(
                command.adminId(),
                command.adminName(),
                command.ipAddress(),
                command.userAgent(),
                "TEAM",
                team.id(),
                beforeValue);
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
        // 직급 코드는 P-prefix 자동 채번 정책을 따르고, 이름 중복만 별도로 막는다.
        ensurePositionNameUnique(name, null);
        ensurePositionSortOrderUnique(command.sortOrder(), null);
        String code = nextPositionCode();
        // 직급은 독립 기준정보이므로 이름/코드를 전역으로 유니크하게 관리한다.
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
        ensurePositionNameUnique(name, position.id());
        ensurePositionSortOrderUnique(command.sortOrder(), position.id());
        Position saved =
                positionRepositoryPort.save(
                        new Position(
                                position.id(),
                                name,
                                // 수정 시 code를 바꾸면 기존 식별/엑셀 참조 흐름이 흔들릴 수 있어 서버가 기존 값을 고정한다.
                                position.code(),
                                position.status(),
                                command.sortOrder(),
                                position.createdAt(),
                                Instant.now()));
        if (isPositionSearchDocumentChanged(position, saved)) {
            publishPositionReindex(saved.id(), command.adminId(), "POSITION_UPDATED");
        }
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

    @Transactional
    public void deletePosition(DeletePositionCommand command) {
        Position position =
                positionRepositoryPort
                        .findById(command.positionId())
                        .orElseThrow(
                                () -> {
                                    saveDeleteFailureAudit(
                                            command.adminId(),
                                            command.adminName(),
                                            command.ipAddress(),
                                            command.userAgent(),
                                            "POSITION",
                                            command.positionId(),
                                            null,
                                            "삭제할 직급을 찾을 수 없습니다.");
                                    return new BusinessException(
                                            ErrorCode.COMMON_NOT_FOUND, "Position not found.");
                                });
        String beforeValue = snapshot(toPositionResult(position));

        // 직급 삭제도 사용 중인 회원이 하나라도 있으면 막아야 회원 검색/상세에서 존재하지 않는 직급을 가리키지 않는다.
        if (!userRepositoryPort.findAllByPositionId(position.id()).isEmpty()) {
            saveDeleteFailureAudit(
                    command.adminId(),
                    command.adminName(),
                    command.ipAddress(),
                    command.userAgent(),
                    "POSITION",
                    position.id(),
                    beforeValue,
                    "해당 직급을 사용하는 회원이 있어 삭제할 수 없습니다.");
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT, "해당 직급을 사용하는 회원이 있어 삭제할 수 없습니다.");
        }

        positionRepositoryPort.deleteById(position.id());
        saveDeleteSuccessAudit(
                command.adminId(),
                command.adminName(),
                command.ipAddress(),
                command.userAgent(),
                "POSITION",
                position.id(),
                beforeValue);
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

    private void ensureDepartmentSortOrderUnique(
            UUID affiliateId, Integer sortOrder, UUID departmentId) {
        boolean duplicated =
                departmentId == null
                        ? departmentRepositoryPort.existsByAffiliateIdAndSortOrder(
                                affiliateId, sortOrder)
                        : departmentRepositoryPort.existsByAffiliateIdAndSortOrderAndIdNot(
                                affiliateId, sortOrder, departmentId);
        if (duplicated) {
            throw duplicatedSortOrderException();
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

    private void ensureTeamSortOrderUnique(UUID affiliateId, Integer sortOrder, UUID teamId) {
        boolean duplicated =
                teamId == null
                        ? teamRepositoryPort.existsByAffiliateIdAndSortOrder(affiliateId, sortOrder)
                        : teamRepositoryPort.existsByAffiliateIdAndSortOrderAndIdNot(
                                affiliateId, sortOrder, teamId);
        if (duplicated) {
            throw duplicatedSortOrderException();
        }
    }

    private void ensurePositionNameUnique(String name, UUID positionId) {
        boolean duplicatedName =
                positionId == null
                        ? positionRepositoryPort.existsByName(name)
                        : positionRepositoryPort.existsByNameAndIdNot(name, positionId);
        if (duplicatedName) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "Position name already exists.");
        }
    }

    private void ensurePositionSortOrderUnique(Integer sortOrder, UUID positionId) {
        boolean duplicated =
                positionId == null
                        ? positionRepositoryPort.existsBySortOrder(sortOrder)
                        : positionRepositoryPort.existsBySortOrderAndIdNot(sortOrder, positionId);
        if (duplicated) {
            throw duplicatedSortOrderException();
        }
    }

    private BusinessException duplicatedSortOrderException() {
        return new BusinessException(
                ErrorCode.ORGANIZATION_SORT_ORDER_DUPLICATED,
                "이미 사용 중인 순서입니다. 다른 순서를 입력해 주세요.");
    }

    private String nextDepartmentCode() {
        // 부서 코드는 D-prefix별 최대 번호 다음 값을 써서 중간 빈 번호가 있어도 재사용하지 않는다.
        return OrganizationCodeGenerator.forDepartmentCodes(
                        departmentRepositoryPort.findAll().stream().map(Department::code).toList())
                .nextCode();
    }

    private String nextTeamCode() {
        // 팀 코드는 T-prefix 기준으로 별도 채번한다.
        return OrganizationCodeGenerator.forTeamCodes(
                        teamRepositoryPort.findAll().stream().map(Team::code).toList())
                .nextCode();
    }

    private String nextPositionCode() {
        // 직급 코드는 P-prefix 기준으로 별도 채번한다.
        return OrganizationCodeGenerator.forPositionCodes(
                        positionRepositoryPort.findAll().stream().map(Position::code).toList())
                .nextCode();
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

    private String snapshot(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_INTERNAL_ERROR, "Failed to serialize admin audit snapshot.");
        }
    }

    private void saveDeleteSuccessAudit(
            UUID adminId,
            String adminName,
            String ipAddress,
            String userAgent,
            String targetType,
            UUID targetId,
            String beforeValue) {
        adminAuditLogRepositoryPort.save(
                new AdminAuditLog(
                        UUID.randomUUID(),
                        adminId,
                        adminName,
                        targetType,
                        targetId,
                        null,
                        null,
                        "ORGANIZATION_MASTER_DATA",
                        "DELETE",
                        AuditResult.SUCCESS,
                        beforeValue,
                        null,
                        ipAddress,
                        userAgent,
                        Instant.now()));
    }

    private void saveDeleteFailureAudit(
            UUID adminId,
            String adminName,
            String ipAddress,
            String userAgent,
            String targetType,
            UUID targetId,
            String beforeValue,
            String message) {
        adminAuditLogRepositoryPort.save(
                new AdminAuditLog(
                        UUID.randomUUID(),
                        adminId,
                        adminName,
                        targetType,
                        targetId,
                        null,
                        null,
                        "ORGANIZATION_MASTER_DATA",
                        "DELETE",
                        AuditResult.FAILURE,
                        beforeValue,
                        snapshot(new FailureSnapshot(message)),
                        ipAddress,
                        userAgent,
                        Instant.now()));
    }

    private boolean isAffiliateSearchDocumentChanged(Affiliate before, Affiliate after) {
        return !before.name().equals(after.name());
    }

    private boolean isDepartmentSearchDocumentChanged(Department before, Department after) {
        return !before.name().equals(after.name())
                || !before.affiliateId().equals(after.affiliateId());
    }

    private boolean isTeamSearchDocumentChanged(Team before, Team after) {
        return !before.name().equals(after.name())
                || !before.departmentId().equals(after.departmentId());
    }

    private boolean isPositionSearchDocumentChanged(Position before, Position after) {
        return !before.name().equals(after.name());
    }

    private void publishAffiliateReindex(UUID affiliateId, UUID requestedByUserId, String reason) {
        // 계열사명 변경은 소속 사용자 문서의 affiliateName 필드 전체에 영향을 주므로 사용자별 동기 호출 대신 범위 이벤트로 넘긴다.
        userSearchReindexRequestDispatcher.publishAfterCommit(
                new UserSearchReindexRequestedEvent(
                        reason,
                        false,
                        List.of(),
                        affiliateId,
                        null,
                        null,
                        null,
                        requestedByUserId));
    }

    private void publishDepartmentReindex(
            UUID departmentId, UUID requestedByUserId, String reason) {
        userSearchReindexRequestDispatcher.publishAfterCommit(
                new UserSearchReindexRequestedEvent(
                        reason,
                        false,
                        List.of(),
                        null,
                        departmentId,
                        null,
                        null,
                        requestedByUserId));
    }

    private void publishTeamReindex(UUID teamId, UUID requestedByUserId, String reason) {
        userSearchReindexRequestDispatcher.publishAfterCommit(
                new UserSearchReindexRequestedEvent(
                        reason, false, List.of(), null, null, teamId, null, requestedByUserId));
    }

    private void publishPositionReindex(UUID positionId, UUID requestedByUserId, String reason) {
        userSearchReindexRequestDispatcher.publishAfterCommit(
                new UserSearchReindexRequestedEvent(
                        reason, false, List.of(), null, null, null, positionId, requestedByUserId));
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
            UUID affiliateId, String name, String code, Integer sortOrder, UUID adminId) {}

    public record UpdateAffiliateStatusCommand(UUID affiliateId, String status) {}

    public record CreateDepartmentCommand(
            UUID affiliateId, String name, String status, Integer sortOrder) {}

    public record UpdateDepartmentCommand(
            UUID departmentId, UUID affiliateId, String name, Integer sortOrder, UUID adminId) {}

    public record UpdateDepartmentStatusCommand(UUID departmentId, String status) {}

    public record DeleteDepartmentCommand(
            UUID departmentId,
            UUID adminId,
            String adminName,
            String ipAddress,
            String userAgent) {}

    public record CreateTeamCommand(
            UUID departmentId, String name, String status, Integer sortOrder) {}

    public record UpdateTeamCommand(
            UUID teamId, UUID departmentId, String name, Integer sortOrder, UUID adminId) {}

    public record UpdateTeamStatusCommand(UUID teamId, String status) {}

    public record DeleteTeamCommand(
            UUID teamId, UUID adminId, String adminName, String ipAddress, String userAgent) {}

    public record CreatePositionCommand(String name, String status, Integer sortOrder) {}

    public record UpdatePositionCommand(
            UUID positionId, String name, Integer sortOrder, UUID adminId) {}

    public record UpdatePositionStatusCommand(UUID positionId, String status) {}

    public record DeletePositionCommand(
            UUID positionId, UUID adminId, String adminName, String ipAddress, String userAgent) {}

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

    private record FailureSnapshot(String message) {}
}
