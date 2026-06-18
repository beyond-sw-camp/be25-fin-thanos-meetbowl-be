package com.meetbowl.application.admin;

import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.AffiliateRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.DepartmentRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.PositionRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.TeamRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.UserRow;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.WorkbookRows;
import com.meetbowl.application.admin.excel.OrganizationMembersExcelWorkbookMapper;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.domain.organization.Affiliate;
import com.meetbowl.domain.organization.AffiliateRepositoryPort;
import com.meetbowl.domain.organization.Department;
import com.meetbowl.domain.organization.DepartmentRepositoryPort;
import com.meetbowl.domain.organization.Position;
import com.meetbowl.domain.organization.PositionRepositoryPort;
import com.meetbowl.domain.organization.Team;
import com.meetbowl.domain.organization.TeamRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;

@Service
public class AdminOrganizationMembersExcelUseCase {

    private final AffiliateRepositoryPort affiliateRepositoryPort;
    private final DepartmentRepositoryPort departmentRepositoryPort;
    private final TeamRepositoryPort teamRepositoryPort;
    private final PositionRepositoryPort positionRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final OrganizationMembersExcelWorkbookMapper workbookMapper;
    private final AdminOrganizationMembersExcelApplyService applyService;
    private final AdminOrganizationMembersExcelAuditService auditService;

    public AdminOrganizationMembersExcelUseCase(
            AffiliateRepositoryPort affiliateRepositoryPort,
            DepartmentRepositoryPort departmentRepositoryPort,
            TeamRepositoryPort teamRepositoryPort,
            PositionRepositoryPort positionRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            OrganizationMembersExcelWorkbookMapper workbookMapper,
            AdminOrganizationMembersExcelApplyService applyService,
            AdminOrganizationMembersExcelAuditService auditService) {
        this.affiliateRepositoryPort = affiliateRepositoryPort;
        this.departmentRepositoryPort = departmentRepositoryPort;
        this.teamRepositoryPort = teamRepositoryPort;
        this.positionRepositoryPort = positionRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.workbookMapper = workbookMapper;
        this.applyService = applyService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ExportResult export() {
        // 다운로드는 현재 DB를 그대로 투영하되, 검색 대상이 아닌 권한(GUEST/SYSTEM 등)은 템플릿에서 제외한다.
        List<Affiliate> affiliates =
                affiliateRepositoryPort.findAll().stream().sorted(affiliateSort()).toList();
        List<Department> departments =
                departmentRepositoryPort.findAll().stream().sorted(departmentSort()).toList();
        List<Team> teams = teamRepositoryPort.findAll().stream().sorted(teamSort()).toList();
        List<Position> positions =
                positionRepositoryPort.findAll().stream().sorted(positionSort()).toList();
        List<User> users =
                userRepositoryPort.findAll().stream()
                        .filter(
                                user ->
                                        user.role() == UserRole.ADMIN
                                                || user.role() == UserRole.USER)
                        .sorted(userSort())
                        .toList();

        Map<UUID, String> affiliateNames =
                affiliates.stream().collect(Collectors.toMap(Affiliate::id, Affiliate::name));
        Map<UUID, String> departmentNames =
                departments.stream().collect(Collectors.toMap(Department::id, Department::name));
        Map<UUID, Department> departmentsById =
                departments.stream()
                        .collect(Collectors.toMap(Department::id, department -> department));
        Map<UUID, String> positionNames =
                positions.stream().collect(Collectors.toMap(Position::id, Position::name));

        byte[] bytes =
                workbookMapper.write(
                        new WorkbookRows(
                                affiliates.stream()
                                        .map(
                                                affiliate ->
                                                        new AffiliateRow(
                                                                0,
                                                                affiliate.id().toString(),
                                                                affiliate.name(),
                                                                affiliate.code(),
                                                                affiliate.status().name()))
                                        .toList(),
                                departments.stream()
                                        .map(
                                                department ->
                                                        new DepartmentRow(
                                                                0,
                                                                department.id().toString(),
                                                                affiliateNames.get(
                                                                        department.affiliateId()),
                                                                department.name(),
                                                                department.code(),
                                                                stringify(department.sortOrder()),
                                                                department.status().name()))
                                        .toList(),
                                teams.stream()
                                        .map(
                                                team -> {
                                                    Department department =
                                                            departmentsById.get(
                                                                    team.departmentId());
                                                    return new TeamRow(
                                                            0,
                                                            team.id().toString(),
                                                            department == null
                                                                    ? null
                                                                    : affiliateNames.get(
                                                                            department
                                                                                    .affiliateId()),
                                                            department == null
                                                                    ? null
                                                                    : department.name(),
                                                            team.name(),
                                                            team.code(),
                                                            stringify(team.sortOrder()),
                                                            team.status().name());
                                                })
                                        .toList(),
                                positions.stream()
                                        .map(
                                                position ->
                                                        new PositionRow(
                                                                0,
                                                                position.id().toString(),
                                                                position.name(),
                                                                position.code(),
                                                                stringify(position.sortOrder()),
                                                                position.status().name()))
                                        .toList(),
                                users.stream()
                                        .map(
                                                user ->
                                                        new UserRow(
                                                                0,
                                                                user.id().toString(),
                                                                user.loginId(),
                                                                user.name(),
                                                                user.email(),
                                                                affiliateNames.get(
                                                                        user.affiliateId()),
                                                                departmentNames.get(
                                                                        user.departmentId()),
                                                                resolveTeamName(
                                                                        teams, user.teamId()),
                                                                positionNames.get(
                                                                        user.positionId()),
                                                                user.role().name(),
                                                                user.status().name()))
                                        .toList()));
        return new ExportResult(OrganizationMembersExcelWorkbookMapper.FILE_NAME, bytes);
    }

    public ImportResult importExcel(ImportCommand command) {
        try {
            return applyService.apply(command);
        } catch (BusinessException exception) {
            auditService.saveFailure(
                    command.adminId(),
                    command.adminName(),
                    command.ipAddress(),
                    command.userAgent(),
                    command.fileName(),
                    exception.details(),
                    exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            auditService.saveFailure(
                    command.adminId(),
                    command.adminName(),
                    command.ipAddress(),
                    command.userAgent(),
                    command.fileName(),
                    List.of(),
                    exception.getMessage());
            throw exception;
        }
    }

    private Comparator<Affiliate> affiliateSort() {
        return Comparator.comparing(Affiliate::sortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Affiliate::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Affiliate::id);
    }

    private Comparator<Department> departmentSort() {
        return Comparator.comparing(Department::sortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Department::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Department::id);
    }

    private Comparator<Team> teamSort() {
        return Comparator.comparing(Team::sortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Team::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Team::id);
    }

    private Comparator<Position> positionSort() {
        return Comparator.comparing(Position::sortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Position::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Position::id);
    }

    private Comparator<User> userSort() {
        return Comparator.comparing(User::loginId, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(User::id);
    }

    private String resolveTeamName(List<Team> teams, UUID teamId) {
        if (teamId == null) {
            return null;
        }
        return teams.stream()
                .filter(team -> team.id().equals(teamId))
                .map(Team::name)
                .findFirst()
                .orElse(null);
    }

    private String stringify(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }

    public record ExportResult(String fileName, byte[] fileBytes) {}

    public record ImportCommand(
            byte[] fileBytes,
            String fileName,
            UUID adminId,
            String adminName,
            String ipAddress,
            String userAgent) {}

    public record ImportResult(
            int createdAffiliates,
            int updatedAffiliates,
            int createdDepartments,
            int updatedDepartments,
            int createdTeams,
            int updatedTeams,
            int createdPositions,
            int updatedPositions,
            int createdUsers,
            int updatedUsers) {}
}
