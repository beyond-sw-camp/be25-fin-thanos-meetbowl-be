package com.meetbowl.api.admin;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.meetbowl.api.admin.dto.AdminOrganizationMembersExcelImportResponse;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.ClientIpResolver;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.application.admin.AdminOrganizationMembersExcelUseCase;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.common.response.ApiResponse;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/admin/organization-members/excel")
public class AdminOrganizationMembersExcelController extends BaseController {

    private static final String XLSX_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final AdminOrganizationMembersExcelUseCase useCase;
    private final GlobalPermissionChecker globalPermissionChecker;

    public AdminOrganizationMembersExcelController(
            AdminOrganizationMembersExcelUseCase useCase,
            GlobalPermissionChecker globalPermissionChecker) {
        this.useCase = useCase;
        this.globalPermissionChecker = globalPermissionChecker;
    }

    @GetMapping
    public ResponseEntity<byte[]> download(@CurrentUser AuthenticatedUser admin) {
        requireAdmin(admin);
        // 응답 바디는 현재 DB 상태를 v2 템플릿 구조로 채운 .xlsx 바이너리다.
        AdminOrganizationMembersExcelUseCase.ExportResult result = useCase.export();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(XLSX_MEDIA_TYPE))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(result.fileName())
                                .build()
                                .toString())
                .body(result.fileBytes());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdminOrganizationMembersExcelImportResponse> upload(
            @CurrentUser AuthenticatedUser admin,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request)
            throws IOException {
        requireAdmin(admin);
        // import 계약은 multipart/form-data + file 필드 + .xlsx 확장자만 허용한다.
        validateExcelFile(file);

        return ok(
                AdminOrganizationMembersExcelImportResponse.from(
                        useCase.importExcel(
                                new AdminOrganizationMembersExcelUseCase.ImportCommand(
                                        file.getBytes(),
                                        file.getOriginalFilename(),
                                        admin.userId(),
                                        admin.displayName(),
                                        ClientIpResolver.resolve(request),
                                        request.getHeader("User-Agent")))));
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "엑셀 파일이 비어 있습니다.");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException(ErrorCode.FILE_INVALID_EXTENSION);
        }
    }

    private void requireAdmin(AuthenticatedUser admin) {
        globalPermissionChecker.requireAdmin(admin);
    }
}
