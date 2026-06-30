package com.meetbowl.api.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.meetbowl.api.common.GlobalExceptionHandler;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.AuthenticatedUserAttributes;
import com.meetbowl.api.common.auth.AuthenticatedUserRole;
import com.meetbowl.api.common.auth.CurrentUserArgumentResolver;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.api.config.WebMvcConfig;
import com.meetbowl.application.admin.AdminOrganizationMembersExcelUseCase;
import com.meetbowl.application.auth.AccessTokenValidationService;

@WebMvcTest(AdminOrganizationMembersExcelController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    CurrentUserArgumentResolver.class,
    GlobalExceptionHandler.class,
    WebMvcConfig.class,
    GlobalPermissionChecker.class
})
class AdminOrganizationMembersExcelControllerTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ORGANIZATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminOrganizationMembersExcelUseCase useCase;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;

    @Test
    void downloadReturnsWorkbookBytes() throws Exception {
        given(useCase.export(ORGANIZATION_ID))
                .willReturn(
                        new AdminOrganizationMembersExcelUseCase.ExportResult(
                                "meetbowl_organization_members_template_v2.xlsx",
                                "xlsx-data".getBytes()));

        mockMvc.perform(
                        get("/api/v1/admin/organization-members/excel")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        "attachment; filename=\"meetbowl_organization_members_template_v2.xlsx\""))
                .andExpect(
                        content()
                                .contentType(
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes("xlsx-data".getBytes()));

        verify(useCase).export(ORGANIZATION_ID);
    }

    @Test
    void uploadReturnsCreatedAndUpdatedCounts() throws Exception {
        given(useCase.importExcel(any()))
                .willReturn(
                        new AdminOrganizationMembersExcelUseCase.ImportResult(
                                1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "organization-members.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "excel".getBytes());

        mockMvc.perform(
                        multipart("/api/v1/admin/organization-members/excel/import")
                                .file(file)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .header("User-Agent", "AdminOrganizationMembersExcelControllerTest")
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdAffiliates").value(1))
                .andExpect(jsonPath("$.data.updatedAffiliates").value(2))
                .andExpect(jsonPath("$.data.createdUsers").value(9))
                .andExpect(jsonPath("$.data.updatedUsers").value(10));

        ArgumentCaptor<AdminOrganizationMembersExcelUseCase.ImportCommand> captor =
                ArgumentCaptor.forClass(AdminOrganizationMembersExcelUseCase.ImportCommand.class);
        verify(useCase).importExcel(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(
                "organization-members.xlsx", captor.getValue().fileName());
        org.junit.jupiter.api.Assertions.assertEquals(ADMIN_ID, captor.getValue().adminId());
    }

    @Test
    void uploadRejectsNonXlsxFile() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "organization-members.csv", "text/csv", "csv".getBytes());

        mockMvc.perform(
                        multipart("/api/v1/admin/organization-members/excel/import")
                                .file(file)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN)))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("FILE_INVALID_EXTENSION"));

        verifyNoInteractions(useCase);
    }

    @Test
    void uploadRejectsNonAdminUser() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "organization-members.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "excel".getBytes());

        mockMvc.perform(
                        multipart("/api/v1/admin/organization-members/excel/import")
                                .file(file)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));

        verifyNoInteractions(useCase);
    }

    private AuthenticatedUser authenticatedUser(AuthenticatedUserRole role) {
        return new AuthenticatedUser(ADMIN_ID, ORGANIZATION_ID, role, "Admin");
    }
}
