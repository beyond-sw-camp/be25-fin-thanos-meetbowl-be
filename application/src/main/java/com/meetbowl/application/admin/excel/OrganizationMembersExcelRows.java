package com.meetbowl.application.admin.excel;

import java.util.List;

public final class OrganizationMembersExcelRows {

    private OrganizationMembersExcelRows() {}

    public record WorkbookRows(
            List<AffiliateRow> affiliates,
            List<DepartmentRow> departments,
            List<TeamRow> teams,
            List<PositionRow> positions,
            List<UserRow> users) {}

    public record AffiliateRow(
            int rowNumber,
            String affiliateId,
            String affiliateName,
            String affiliateCode,
            String status) {}

    public record DepartmentRow(
            int rowNumber,
            String departmentId,
            String affiliateName,
            String departmentName,
            String departmentCode,
            String sortNumber,
            String status) {}

    public record TeamRow(
            int rowNumber,
            String teamId,
            String affiliateName,
            String departmentName,
            String teamName,
            String teamCode,
            String sortNumber,
            String status) {}

    public record PositionRow(
            int rowNumber,
            String positionId,
            String affiliateName,
            String positionName,
            String positionCode,
            String sortNumber,
            String status) {}

    public record UserRow(
            int rowNumber,
            String userId,
            String loginId,
            String name,
            String email,
            String affiliateName,
            String departmentName,
            String teamName,
            String positionName,
            String role,
            String status) {}
}
