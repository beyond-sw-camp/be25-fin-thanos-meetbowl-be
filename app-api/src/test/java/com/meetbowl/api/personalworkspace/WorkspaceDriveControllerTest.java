package com.meetbowl.api.personalworkspace;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.meetbowl.api.common.GlobalExceptionHandler;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.AuthenticatedUserAttributes;
import com.meetbowl.api.common.auth.AuthenticatedUserRole;
import com.meetbowl.api.common.auth.CurrentUserArgumentResolver;
import com.meetbowl.api.config.WebMvcConfig;
import com.meetbowl.application.auth.AccessTokenValidationService;
import com.meetbowl.application.personalworkspace.drive.DeleteDriveFileUseCase;
import com.meetbowl.application.personalworkspace.drive.DownloadDriveFileUseCase;
import com.meetbowl.application.personalworkspace.drive.DriveFileResult;
import com.meetbowl.application.personalworkspace.drive.GetDriveFileUseCase;
import com.meetbowl.application.personalworkspace.drive.GetDriveFilesUseCase;
import com.meetbowl.application.personalworkspace.drive.RegisterDriveFileCommand;
import com.meetbowl.application.personalworkspace.drive.RegisterDriveFileUseCase;

@WebMvcTest(controllers = WorkspaceDriveController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CurrentUserArgumentResolver.class, GlobalExceptionHandler.class, WebMvcConfig.class})
class WorkspaceDriveControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;
    @MockitoBean private GetDriveFilesUseCase getDriveFilesUseCase;
    @MockitoBean private RegisterDriveFileUseCase registerDriveFileUseCase;
    @MockitoBean private GetDriveFileUseCase getDriveFileUseCase;
    @MockitoBean private DownloadDriveFileUseCase downloadDriveFileUseCase;
    @MockitoBean private DeleteDriveFileUseCase deleteDriveFileUseCase;

    @Test
    void uploadFile_passesMultipartContentToUseCase() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        byte[] content = "sample pdf".getBytes();
        AuthenticatedUser user =
                new AuthenticatedUser(userId, organizationId, AuthenticatedUserRole.USER, "사용자");
        MockMultipartFile file =
                new MockMultipartFile("file", "sample.pdf", "application/pdf", content);
        when(registerDriveFileUseCase.execute(any()))
                .thenReturn(
                        new DriveFileResult(
                                fileId,
                                userId,
                                "sample.pdf",
                                "application/pdf",
                                content.length,
                                "personal-drive/" + userId + "/file",
                                Instant.parse("2026-06-15T00:00:00Z")));

        mockMvc.perform(
                        multipart("/api/v1/workspace/drive/files")
                                .file(file)
                                .requestAttr(AuthenticatedUserAttributes.CURRENT_USER, user))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileId").value(fileId.toString()));

        ArgumentCaptor<RegisterDriveFileCommand> commandCaptor =
                ArgumentCaptor.forClass(RegisterDriveFileCommand.class);
        verify(registerDriveFileUseCase).execute(commandCaptor.capture());
        RegisterDriveFileCommand command = commandCaptor.getValue();
        assertEquals(userId, command.ownerUserId());
        assertEquals(organizationId, command.organizationId());
        assertEquals("sample.pdf", command.originalFileName());
        assertArrayEquals(content, command.content());
    }
}
