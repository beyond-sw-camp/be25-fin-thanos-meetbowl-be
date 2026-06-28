package com.meetbowl.application.admin.excel;

import java.util.List;

public final class OrganizationMembersExcelRows {

    private OrganizationMembersExcelRows() {}

    public record WorkbookRows(
            List<DepartmentRow> departments,
            List<TeamRow> teams,
            List<PositionRow> positions,
            List<UserRow> users) {}

    public record DepartmentRow(
            int rowNumber,
            String departmentId,
            String departmentName,
            String sortNumber,
            String status) {}

    public record TeamRow(
            int rowNumber,
            String teamId,
            String departmentName,
            String teamName,
            String sortNumber,
            String status) {}

    public record PositionRow(
            int rowNumber,
            String positionId,
            String positionName,
            String sortNumber,
            String status) {}

    public record UserRow(
            int rowNumber,
            String userId,
            String loginId,
            String name,
            String email,
            String departmentName,
            String teamName,
            String positionName,
            String status) {}
}
