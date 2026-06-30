package com.meetbowl.application.admin;

import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.DepartmentRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.PositionRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.TeamRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.UserRow;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.WorkbookRows;
import com.meetbowl.application.admin.excel.OrganizationMembersExcelWorkbookMapper;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
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

    private final DepartmentRepositoryPort departmentRepositoryPort;
    private final TeamRepositoryPort teamRepositoryPort;
    private final PositionRepositoryPort positionRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final OrganizationMembersExcelWorkbookMapper workbookMapper;
    private final AdminOrganizationMembersExcelApplyService applyService;
    private final AdminOrganizationMembersExcelAuditService auditService;

    public AdminOrganizationMembersExcelUseCase(
            DepartmentRepositoryPort departmentRepositoryPort,
            TeamRepositoryPort teamRepositoryPort,
            PositionRepositoryPort positionRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            OrganizationMembersExcelWorkbookMapper workbookMapper,
            AdminOrganizationMembersExcelApplyService applyService,
            AdminOrganizationMembersExcelAuditService auditService) {
        this.departmentRepositoryPort = departmentRepositoryPort;
        this.teamRepositoryPort = teamRepositoryPort;
        this.positionRepositoryPort = positionRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.workbookMapper = workbookMapper;
        this.applyService = applyService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ExportResult export(UUID adminAffiliateId) {
        List<Department> departments =
                departmentRepositoryPort.findAllForExcelExport().stream()
                        .filter(department -> adminAffiliateId.equals(department.affiliateId()))
                        .toList();
        Set<UUID> departmentIds = departments.stream().map(Department::id).collect(java.util.stream.Collectors.toSet());
        List<Team> teams =
                teamRepositoryPort.findAllForExcelExport().stream()
                        .filter(team -> departmentIds.contains(team.departmentId()))
                        .toList();
        List<Position> positions =
                positionRepositoryPort.findAllForExcelExport().stream()
                        .filter(position -> adminAffiliateId.equals(position.affiliateId()))
                        .toList();
        List<User> users =
                userRepositoryPort.findAllForExcelExportByRoles(Set.of(UserRole.USER))
                        .stream()
                        .filter(user -> adminAffiliateId.equals(user.affiliateId()))
                        .toList();
        java.util.Map<UUID, String> departmentNames =
                departments.stream().collect(java.util.stream.Collectors.toMap(Department::id, Department::name));
        java.util.Map<UUID, String> positionNames =
                positions.stream().collect(java.util.stream.Collectors.toMap(Position::id, Position::name));

        byte[] bytes =
                workbookMapper.write(
                        new WorkbookRows(
                                departments.stream()
                                        .map(
                                                department ->
                                                        new DepartmentRow(
                                                                0,
                                                                department.id().toString(),
                                                                department.name(),
                                                                stringify(department.sortOrder()),
                                                                department.status().name()))
                                        .toList(),
                                teams.stream()
                                        .map(
                                                team ->
                                                        new TeamRow(
                                                                0,
                                                                team.id().toString(),
                                                                        departments.stream()
                                                                                .filter(item -> item.id().equals(team.departmentId()))
                                                                                .map(Department::name)
                                                                                .findFirst()
                                                                                .orElse(null),
                                                                team.name(),
                                                                stringify(team.sortOrder()),
                                                                team.status().name()))
                                        .toList(),
                                positions.stream()
                                        .map(
                                                position ->
                                                        new PositionRow(
                                                                0,
                                                                position.id().toString(),
                                                                position.name(),
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
                                                                departmentNames.get(
                                                                        user.departmentId()),
                                                                resolveTeamName(teams, user.teamId()),
                                                                positionNames.get(
                                                                        user.positionId()),
                                                                user.status().name()))
                                        .toList()));
        String fileName =
                OrganizationMembersExcelWorkbookMapper.FILE_NAME.replace(
                        ".xlsx",
                        "_" + sanitizeFileName(adminAffiliateId.toString()) + ".xlsx");
        return new ExportResult(fileName, bytes);
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

    private String sanitizeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "affiliate";
        }
        return value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    public record ExportResult(String fileName, byte[] fileBytes) {}

    public record ImportCommand(
            byte[] fileBytes,
            String fileName,
            UUID adminAffiliateId,
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
