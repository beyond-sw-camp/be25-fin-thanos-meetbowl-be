package com.meetbowl.application.personalworkspace.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFile;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFileRepositoryPort;

/** 드라이브 파일 등록 시 저장 경로 생성, 다운로드/삭제의 소유자 검증과 멱등 처리를 검증한다. */
class DriveUseCaseTest {

    private final PersonalWorkspaceDriveFileRepositoryPort filePort =
            Mockito.mock(PersonalWorkspaceDriveFileRepositoryPort.class);

    private PersonalWorkspaceDriveFile activeFileOf(UUID id, UUID ownerUserId) {
        return PersonalWorkspaceDriveFile.of(
                id,
                ownerUserId,
                "spec.pdf",
                "application/pdf",
                2048L,
                "personal-drive/" + ownerUserId + "/" + UUID.randomUUID(),
                Instant.parse("2026-06-01T00:00:00Z"),
                null);
    }

    @Test
    void registerFile_generates_owner_scoped_storage_key() {
        RegisterDriveFileUseCase useCase = new RegisterDriveFileUseCase(filePort);
        UUID userId = UUID.randomUUID();
        when(filePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DriveFileResult result =
                useCase.execute(
                        new RegisterDriveFileCommand(userId, "spec.pdf", "application/pdf", 2048L));

        assertEquals(userId, result.ownerUserId());
        assertTrue(result.storageKey().startsWith("personal-drive/" + userId + "/"));
    }

    @Test
    void getFile_fail_when_not_owner() {
        GetDriveFileUseCase useCase = new GetDriveFileUseCase(filePort);
        UUID userId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        when(filePort.findById(fileId))
                .thenReturn(Optional.of(activeFileOf(fileId, UUID.randomUUID())));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> useCase.execute(userId, fileId));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, ex.errorCode());
    }

    @Test
    void deleteFile_is_idempotent_when_already_deleted() {
        DeleteDriveFileUseCase useCase = new DeleteDriveFileUseCase(filePort);
        UUID userId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        PersonalWorkspaceDriveFile deleted =
                activeFileOf(fileId, userId).delete(Instant.parse("2026-06-02T00:00:00Z"));
        when(filePort.findById(fileId)).thenReturn(Optional.of(deleted));

        useCase.execute(userId, fileId);

        verify(filePort, never()).save(any());
    }
}
