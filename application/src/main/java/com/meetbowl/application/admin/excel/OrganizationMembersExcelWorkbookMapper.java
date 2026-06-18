package com.meetbowl.application.admin.excel;

import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.AffiliateRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.DepartmentRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.PositionRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.TeamRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.UserRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.WorkbookRows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.common.response.ErrorDetail;

@Component
public class OrganizationMembersExcelWorkbookMapper {

    public static final String FILE_NAME = "meetbowl_organization_members_template_v2.xlsx";
    public static final String GUIDE_SHEET = "작성가이드";
    public static final String AFFILIATE_SHEET = "계열사";
    public static final String DEPARTMENT_SHEET = "부서";
    public static final String TEAM_SHEET = "팀";
    public static final String POSITION_SHEET = "직급";
    public static final String USER_SHEET = "회원";
    public static final String REFERENCE_SHEET = "참조값";

    private static final int HEADER_ROW_INDEX = 3;
    private static final int DATA_START_ROW_INDEX = 4;
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    // v2 템플릿은 헤더 셀에 "한글명\n(fieldName)" 형태를 쓰므로, import 시에는 괄호 안 fieldName만 계약으로 사용한다.
    private static final List<String> AFFILIATE_COLUMNS =
            List.of("affiliateId", "affiliateName", "affiliateCode", "status");
    private static final List<String> DEPARTMENT_COLUMNS =
            List.of(
                    "departmentId",
                    "affiliateName",
                    "departmentName",
                    "departmentCode",
                    "sortNumber",
                    "status");
    private static final List<String> TEAM_COLUMNS =
            List.of(
                    "teamId",
                    "affiliateName",
                    "departmentName",
                    "teamName",
                    "teamCode",
                    "sortNumber",
                    "status");
    private static final List<String> POSITION_COLUMNS =
            List.of("positionId", "positionName", "positionCode", "sortNumber", "status");
    private static final List<String> USER_COLUMNS =
            List.of(
                    "userId",
                    "loginId",
                    "name",
                    "email",
                    "affiliateName",
                    "departmentName",
                    "teamName",
                    "positionName",
                    "role",
                    "status");

    public byte[] write(WorkbookRows rows) {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            writeGuideSheet(workbook, styles);
            writeAffiliateSheet(workbook, styles, rows.affiliates());
            writeDepartmentSheet(workbook, styles, rows.departments());
            writeTeamSheet(workbook, styles, rows.teams());
            writePositionSheet(workbook, styles, rows.positions());
            writeUserSheet(workbook, styles, rows.users());
            writeReferenceSheet(workbook, styles);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "엑셀 파일을 생성할 수 없습니다.");
        }
    }

    public WorkbookRows read(byte[] fileBytes) {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
            // 작성가이드/참조값 시트는 안내·드롭다운용이므로 import 대상에서 제외한다.
            return new WorkbookRows(
                    readAffiliates(workbook),
                    readDepartments(workbook),
                    readTeams(workbook),
                    readPositions(workbook),
                    readUsers(workbook));
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "엑셀 파일을 읽을 수 없습니다.");
        }
    }

    private List<AffiliateRow> readAffiliates(Workbook workbook) {
        Sheet sheet = requireSheet(workbook, AFFILIATE_SHEET);
        Map<String, Integer> columns = headerColumns(sheet, AFFILIATE_SHEET, AFFILIATE_COLUMNS);
        List<AffiliateRow> rows = new ArrayList<>();
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, columns)) {
                continue;
            }
            rows.add(
                    new AffiliateRow(
                            rowIndex + 1,
                            value(row, columns.get("affiliateId")),
                            value(row, columns.get("affiliateName")),
                            value(row, columns.get("affiliateCode")),
                            value(row, columns.get("status"))));
        }
        return rows;
    }

    private List<DepartmentRow> readDepartments(Workbook workbook) {
        Sheet sheet = requireSheet(workbook, DEPARTMENT_SHEET);
        Map<String, Integer> columns = headerColumns(sheet, DEPARTMENT_SHEET, DEPARTMENT_COLUMNS);
        List<DepartmentRow> rows = new ArrayList<>();
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, columns)) {
                continue;
            }
            rows.add(
                    new DepartmentRow(
                            rowIndex + 1,
                            value(row, columns.get("departmentId")),
                            value(row, columns.get("affiliateName")),
                            value(row, columns.get("departmentName")),
                            value(row, columns.get("departmentCode")),
                            value(row, columns.get("sortNumber")),
                            value(row, columns.get("status"))));
        }
        return rows;
    }

    private List<TeamRow> readTeams(Workbook workbook) {
        Sheet sheet = requireSheet(workbook, TEAM_SHEET);
        Map<String, Integer> columns = headerColumns(sheet, TEAM_SHEET, TEAM_COLUMNS);
        List<TeamRow> rows = new ArrayList<>();
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, columns)) {
                continue;
            }
            rows.add(
                    new TeamRow(
                            rowIndex + 1,
                            value(row, columns.get("teamId")),
                            value(row, columns.get("affiliateName")),
                            value(row, columns.get("departmentName")),
                            value(row, columns.get("teamName")),
                            value(row, columns.get("teamCode")),
                            value(row, columns.get("sortNumber")),
                            value(row, columns.get("status"))));
        }
        return rows;
    }

    private List<PositionRow> readPositions(Workbook workbook) {
        Sheet sheet = requireSheet(workbook, POSITION_SHEET);
        Map<String, Integer> columns = headerColumns(sheet, POSITION_SHEET, POSITION_COLUMNS);
        List<PositionRow> rows = new ArrayList<>();
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, columns)) {
                continue;
            }
            rows.add(
                    new PositionRow(
                            rowIndex + 1,
                            value(row, columns.get("positionId")),
                            value(row, columns.get("positionName")),
                            value(row, columns.get("positionCode")),
                            value(row, columns.get("sortNumber")),
                            value(row, columns.get("status"))));
        }
        return rows;
    }

    private List<UserRow> readUsers(Workbook workbook) {
        Sheet sheet = requireSheet(workbook, USER_SHEET);
        Map<String, Integer> columns = headerColumns(sheet, USER_SHEET, USER_COLUMNS);
        List<UserRow> rows = new ArrayList<>();
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, columns)) {
                continue;
            }
            rows.add(
                    new UserRow(
                            rowIndex + 1,
                            value(row, columns.get("userId")),
                            value(row, columns.get("loginId")),
                            value(row, columns.get("name")),
                            value(row, columns.get("email")),
                            value(row, columns.get("affiliateName")),
                            value(row, columns.get("departmentName")),
                            value(row, columns.get("teamName")),
                            value(row, columns.get("positionName")),
                            value(row, columns.get("role")),
                            value(row, columns.get("status"))));
        }
        return rows;
    }

    private Sheet requireSheet(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw validationFailure(
                    List.of(new ErrorDetail(sheetName, null, "sheetName", "필수 시트가 없습니다.")));
        }
        return sheet;
    }

    private Map<String, Integer> headerColumns(
            Sheet sheet, String sheetName, List<String> requiredColumns) {
        Row headerRow = sheet.getRow(HEADER_ROW_INDEX);
        Map<String, Integer> columns = new LinkedHashMap<>();
        if (headerRow != null) {
            for (Cell cell : headerRow) {
                String key = headerKey(DATA_FORMATTER.formatCellValue(cell));
                if (!key.isBlank()) {
                    columns.put(key, cell.getColumnIndex());
                }
            }
        }

        List<ErrorDetail> details = new ArrayList<>();
        for (String column : requiredColumns) {
            if (!columns.containsKey(column)) {
                details.add(
                        new ErrorDetail(sheetName, HEADER_ROW_INDEX + 1, column, "필수 컬럼이 없습니다."));
            }
        }
        if (!details.isEmpty()) {
            throw validationFailure(details);
        }
        return columns;
    }

    private String headerKey(String headerText) {
        if (headerText == null) {
            return "";
        }
        int start = headerText.indexOf('(');
        int end = headerText.indexOf(')');
        if (start >= 0 && end > start) {
            return headerText.substring(start + 1, end).trim();
        }
        return headerText.trim();
    }

    private boolean isBlankRow(Row row, Map<String, Integer> columns) {
        if (row == null) {
            return true;
        }
        return columns.values().stream().allMatch(index -> value(row, index).isBlank());
    }

    private String value(Row row, Integer columnIndex) {
        if (row == null || columnIndex == null) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return "";
        }
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private void writeGuideSheet(Workbook workbook, Styles styles) {
        Sheet sheet = workbook.createSheet(GUIDE_SHEET);
        writeRow(sheet, 0, styles.titleStyle(), "Meetbowl 조직/회원 일괄 등록·수정 엑셀 양식");
        writeRow(sheet, 1, styles.defaultStyle(), "계열사, 부서, 팀, 직급, 회원 정보를 한 번에 등록·수정하기 위한 템플릿입니다.");
        writeRow(sheet, 2, styles.headerStyle(), "구분", "내용", "필수 여부", "입력 예시", "주의");
        writeRow(
                sheet,
                3,
                styles.defaultStyle(),
                "전체",
                "엑셀에 없는 기존 데이터는 삭제되지 않습니다.",
                "필수",
                "-",
                "삭제 기능은 이번 범위에서 제외");
        writeRow(
                sheet,
                4,
                styles.defaultStyle(),
                "전체",
                "id 컬럼은 기존 데이터 식별용입니다.",
                "선택",
                "UUID",
                "신규 생성 시 비워둬도 됨");
        writeRow(
                sheet,
                5,
                styles.defaultStyle(),
                "계열사",
                "계열사명, 계열사 코드, 상태를 입력합니다.",
                "필수",
                "한화시스템 / HSC / ACTIVE",
                "계열사명·코드 중복 주의");
        writeRow(
                sheet,
                6,
                styles.defaultStyle(),
                "부서",
                "계열사 하위 부서를 입력합니다.",
                "필수",
                "한화시스템 / 서비스개발부 / DEV / 1 / ACTIVE",
                "sortNumber 필수");
        writeRow(
                sheet,
                7,
                styles.defaultStyle(),
                "팀",
                "부서 하위 팀을 입력합니다.",
                "필수",
                "한화시스템 / 서비스개발부 / 플랫폼개발팀 / PLATFORM / 1 / ACTIVE",
                "sortNumber 필수");
        writeRow(
                sheet,
                8,
                styles.defaultStyle(),
                "직급",
                "직급과 코드, 순서를 입력합니다.",
                "필수",
                "부장 / GENERAL_MANAGER / 3 / ACTIVE",
                "sortNumber 필수");
        writeRow(
                sheet,
                9,
                styles.defaultStyle(),
                "회원",
                "비밀번호 없이 로그인 ID, 조직, 권한, 상태를 입력합니다.",
                "필수",
                "user01 / USER / ACTIVE",
                "신규 회원은 초기 비밀번호 1234 사용");
        autosize(sheet, 5);
    }

    private void writeAffiliateSheet(Workbook workbook, Styles styles, List<AffiliateRow> rows) {
        Sheet sheet = workbook.createSheet(AFFILIATE_SHEET);
        writeRow(sheet, 0, styles.titleStyle(), "계열사");
        writeRow(sheet, 1, styles.defaultStyle(), "계열사 마스터를 등록·수정합니다. 신규 행은 ID를 비워두세요.");
        writeRow(
                sheet,
                2,
                styles.defaultStyle(),
                "주황색=필수 입력 / 파란색=선택·식별용 / 회색=id·읽기용  |  헤더명은 변경하지 마세요.");
        writeHeaderRow(
                sheet,
                styles,
                3,
                "계열사 ID\n(affiliateId)",
                "계열사명\n(affiliateName)",
                "계열사 코드\n(affiliateCode)",
                "상태\n(status)");
        int rowIndex = 4;
        for (AffiliateRow row : rows) {
            writeRow(
                    sheet,
                    rowIndex++,
                    styles.defaultStyle(),
                    row.affiliateId(),
                    row.affiliateName(),
                    row.affiliateCode(),
                    row.status());
        }
        autosize(sheet, 4);
    }

    private void writeDepartmentSheet(Workbook workbook, Styles styles, List<DepartmentRow> rows) {
        Sheet sheet = workbook.createSheet(DEPARTMENT_SHEET);
        writeRow(sheet, 0, styles.titleStyle(), "부서");
        writeRow(sheet, 1, styles.defaultStyle(), "계열사 하위 부서를 등록·수정합니다. sortNumber는 화면 표시 순서입니다.");
        writeRow(
                sheet,
                2,
                styles.defaultStyle(),
                "주황색=필수 입력 / 파란색=선택·식별용 / 회색=id·읽기용  |  헤더명은 변경하지 마세요.");
        writeHeaderRow(
                sheet,
                styles,
                3,
                "부서 ID\n(departmentId)",
                "계열사명\n(affiliateName)",
                "부서명\n(departmentName)",
                "부서 코드\n(departmentCode)",
                "순서\n(sortNumber)",
                "상태\n(status)");
        int rowIndex = 4;
        for (DepartmentRow row : rows) {
            writeRow(
                    sheet,
                    rowIndex++,
                    styles.defaultStyle(),
                    row.departmentId(),
                    row.affiliateName(),
                    row.departmentName(),
                    row.departmentCode(),
                    row.sortNumber(),
                    row.status());
        }
        autosize(sheet, 6);
    }

    private void writeTeamSheet(Workbook workbook, Styles styles, List<TeamRow> rows) {
        Sheet sheet = workbook.createSheet(TEAM_SHEET);
        writeRow(sheet, 0, styles.titleStyle(), "팀");
        writeRow(sheet, 1, styles.defaultStyle(), "부서 하위 팀을 등록·수정합니다. 부서명과 계열사명을 함께 입력하세요.");
        writeRow(
                sheet,
                2,
                styles.defaultStyle(),
                "주황색=필수 입력 / 파란색=선택·식별용 / 회색=id·읽기용  |  헤더명은 변경하지 마세요.");
        writeHeaderRow(
                sheet,
                styles,
                3,
                "팀 ID\n(teamId)",
                "계열사명\n(affiliateName)",
                "부서명\n(departmentName)",
                "팀명\n(teamName)",
                "팀 코드\n(teamCode)",
                "순서\n(sortNumber)",
                "상태\n(status)");
        int rowIndex = 4;
        for (TeamRow row : rows) {
            writeRow(
                    sheet,
                    rowIndex++,
                    styles.defaultStyle(),
                    row.teamId(),
                    row.affiliateName(),
                    row.departmentName(),
                    row.teamName(),
                    row.teamCode(),
                    row.sortNumber(),
                    row.status());
        }
        autosize(sheet, 7);
    }

    private void writePositionSheet(Workbook workbook, Styles styles, List<PositionRow> rows) {
        Sheet sheet = workbook.createSheet(POSITION_SHEET);
        writeRow(sheet, 0, styles.titleStyle(), "직급");
        writeRow(sheet, 1, styles.defaultStyle(), "직급 마스터를 등록·수정합니다. sortNumber는 화면 표시 순서입니다.");
        writeRow(
                sheet,
                2,
                styles.defaultStyle(),
                "주황색=필수 입력 / 파란색=선택·식별용 / 회색=id·읽기용  |  헤더명은 변경하지 마세요.");
        writeHeaderRow(
                sheet,
                styles,
                3,
                "직급 ID\n(positionId)",
                "직급명\n(positionName)",
                "직급 코드\n(positionCode)",
                "순서\n(sortNumber)",
                "상태\n(status)");
        int rowIndex = 4;
        for (PositionRow row : rows) {
            writeRow(
                    sheet,
                    rowIndex++,
                    styles.defaultStyle(),
                    row.positionId(),
                    row.positionName(),
                    row.positionCode(),
                    row.sortNumber(),
                    row.status());
        }
        autosize(sheet, 5);
    }

    private void writeUserSheet(Workbook workbook, Styles styles, List<UserRow> rows) {
        Sheet sheet = workbook.createSheet(USER_SHEET);
        writeRow(sheet, 0, styles.titleStyle(), "회원");
        writeRow(sheet, 1, styles.defaultStyle(), "회원 계정과 조직 매핑 정보를 등록·수정합니다. 비밀번호는 입력하지 않습니다.");
        writeRow(
                sheet,
                2,
                styles.defaultStyle(),
                "주황색=필수 입력 / 파란색=선택·식별용 / 회색=id·읽기용  |  헤더명은 변경하지 마세요.");
        writeHeaderRow(
                sheet,
                styles,
                3,
                "회원 ID\n(userId)",
                "로그인 ID\n(loginId)",
                "이름\n(name)",
                "이메일\n(email)",
                "계열사명\n(affiliateName)",
                "부서명\n(departmentName)",
                "팀명\n(teamName)",
                "직급명\n(positionName)",
                "권한\n(role)",
                "상태\n(status)");
        int rowIndex = 4;
        for (UserRow row : rows) {
            writeRow(
                    sheet,
                    rowIndex++,
                    styles.defaultStyle(),
                    row.userId(),
                    row.loginId(),
                    row.name(),
                    row.email(),
                    row.affiliateName(),
                    row.departmentName(),
                    row.teamName(),
                    row.positionName(),
                    row.role(),
                    row.status());
        }
        autosize(sheet, 10);
    }

    private void writeReferenceSheet(Workbook workbook, Styles styles) {
        Sheet sheet = workbook.createSheet(REFERENCE_SHEET);
        writeRow(sheet, 0, styles.titleStyle(), "참조값");
        writeRow(sheet, 1, styles.defaultStyle(), "드롭다운과 검증에 사용하는 값입니다. 직접 수정하지 않는 것을 권장합니다.");
        writeRow(sheet, 2, styles.headerStyle(), "status", "role", "설명");
        writeRow(sheet, 3, styles.defaultStyle(), "ACTIVE", "ADMIN", "활성 / 관리자");
        writeRow(sheet, 4, styles.defaultStyle(), "INACTIVE", "USER", "비활성 / 일반 사용자");
        autosize(sheet, 3);
    }

    private void writeHeaderRow(Sheet sheet, Styles styles, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int index = 0; index < values.length; index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(values[index]);
            cell.setCellStyle(styles.headerStyle());
        }
        sheet.createFreezePane(0, DATA_START_ROW_INDEX);
    }

    private void writeRow(Sheet sheet, int rowIndex, CellStyle style, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int index = 0; index < values.length; index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(values[index] == null ? "" : values[index]);
            cell.setCellStyle(style);
        }
    }

    private void autosize(Sheet sheet, int columnCount) {
        for (int index = 0; index < columnCount; index++) {
            sheet.autoSizeColumn(index);
            sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 1024, 14000));
        }
    }

    private BusinessException validationFailure(List<ErrorDetail> details) {
        return new BusinessException(ErrorCode.VALIDATION_FAILED, details);
    }

    private record Styles(CellStyle titleStyle, CellStyle headerStyle, CellStyle defaultStyle) {

        private Styles(Workbook workbook) {
            this(titleStyleOf(workbook), headerStyleOf(workbook), defaultStyleOf(workbook));
        }

        private static CellStyle titleStyleOf(Workbook workbook) {
            Font font = workbook.createFont();
            font.setBold(true);
            font.setFontHeightInPoints((short) 12);
            CellStyle style = workbook.createCellStyle();
            style.setFont(font);
            return style;
        }

        private static CellStyle headerStyleOf(Workbook workbook) {
            Font font = workbook.createFont();
            font.setBold(true);
            CellStyle style = workbook.createCellStyle();
            style.setFont(font);
            style.setWrapText(true);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return style;
        }

        private static CellStyle defaultStyleOf(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setWrapText(true);
            return style;
        }
    }
}
