package com.meetbowl.application.personalworkspace.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackup;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupRepositoryPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupSourceType;

class GetBackupDetailUseCaseTest {

    private final PersonalWorkspaceBackupRepositoryPort repository =
            Mockito.mock(PersonalWorkspaceBackupRepositoryPort.class);
    private final GetBackupDetailUseCase useCase = new GetBackupDetailUseCase(repository);

    @Test
    void returnsOwnedBackupBody() {
        UUID userId = UUID.randomUUID();
        UUID backupId = UUID.randomUUID();
        PersonalWorkspaceBackup backup = backup(backupId, userId);
        when(repository.findById(backupId)).thenReturn(Optional.of(backup));

        BackupDetailResult result = useCase.execute(userId, backupId);

        assertEquals("메일 본문", result.body());
        assertEquals(backup.sourceId(), result.sourceId());
    }

    @Test
    void rejectsAnotherUsersBackup() {
        UUID backupId = UUID.randomUUID();
        when(repository.findById(backupId))
                .thenReturn(Optional.of(backup(backupId, UUID.randomUUID())));

        assertThrows(
                BusinessException.class,
                () -> useCase.execute(UUID.randomUUID(), backupId));
    }

    private PersonalWorkspaceBackup backup(UUID backupId, UUID ownerUserId) {
        return PersonalWorkspaceBackup.of(
                backupId,
                ownerUserId,
                PersonalWorkspaceBackupSourceType.MAIL,
                UUID.randomUUID(),
                "메일 제목",
                "메일 요약",
                "메일 본문",
                List.of(),
                Instant.parse("2026-06-24T00:00:00Z"));
    }
}
