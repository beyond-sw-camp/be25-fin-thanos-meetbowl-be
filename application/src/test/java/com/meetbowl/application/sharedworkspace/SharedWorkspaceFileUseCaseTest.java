package com.meetbowl.application.sharedworkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.domain.sharedworkspace.DocumentVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersionRepositoryPort;

class SharedWorkspaceFileUseCaseTest {

    private static final Instant NOW = Instant.parse("2099-01-01T01:00:00Z");

    @Test
    void createFileAlsoCreatesInitialVersion() {
        InMemoryFileRepository fileRepository = new InMemoryFileRepository();
        InMemoryVersionRepository versionRepository = new InMemoryVersionRepository();
        CreateSharedWorkspaceFileUseCase useCase =
                new CreateSharedWorkspaceFileUseCase(
                        fileRepository, versionRepository, Clock.fixed(NOW, ZoneOffset.UTC));

        SharedWorkspaceFileResult result =
                useCase.execute(
                        new CreateSharedWorkspaceFileCommand(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "기획서.pdf",
                                "application/pdf",
                                1024L,
                                "shared/workspace/file.pdf",
                                "초안"));

        assertEquals(DocumentVersion.INITIAL, result.currentVersion());
        assertEquals(DocumentVersion.INITIAL, versionRepository.versions.getFirst().version());
    }

    @Test
    void rejectSaveWhenExpectedVersionIsStale() {
        InMemoryFileRepository fileRepository = new InMemoryFileRepository();
        InMemoryVersionRepository versionRepository = new InMemoryVersionRepository();
        UUID workspaceId = UUID.randomUUID();
        UUID uploaderUserId = UUID.randomUUID();
        SharedWorkspaceFile saved =
                fileRepository.save(
                        SharedWorkspaceFile.create(
                                        workspaceId,
                                        uploaderUserId,
                                        "기획서.pdf",
                                        "application/pdf",
                                        1024L,
                                        "shared/workspace/file.pdf",
                                        NOW)
                                .addVersion(
                                        uploaderUserId,
                                        "기획서.pdf",
                                        "application/pdf",
                                        2048L,
                                        "shared/workspace/file-1.1.0.pdf",
                                        DocumentVersion.INITIAL,
                                        DocumentVersion.parse("1.1.0"),
                                        NOW));
        AddSharedWorkspaceFileVersionUseCase useCase =
                new AddSharedWorkspaceFileVersionUseCase(
                        fileRepository, versionRepository, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(
                BusinessException.class,
                () ->
                        useCase.execute(
                                new AddSharedWorkspaceFileVersionCommand(
                                        workspaceId,
                                        saved.id(),
                                        uploaderUserId,
                                        "기획서.pdf",
                                        "application/pdf",
                                        3072L,
                                        "shared/workspace/file-1.2.0.pdf",
                                        "수정",
                                        DocumentVersion.INITIAL,
                                        DocumentVersion.parse("1.2.0"))));
        assertEquals(0, versionRepository.versions.size());
    }

    private static final class InMemoryFileRepository implements SharedWorkspaceFileRepositoryPort {
        private final Map<UUID, SharedWorkspaceFile> files = new HashMap<>();

        @Override
        public SharedWorkspaceFile save(SharedWorkspaceFile file) {
            UUID id = file.id() == null ? UUID.randomUUID() : file.id();
            SharedWorkspaceFile saved =
                    SharedWorkspaceFile.of(
                            id,
                            file.workspaceId(),
                            file.uploaderUserId(),
                            file.originalFileName(),
                            file.contentType(),
                            file.sizeBytes(),
                            file.storageKey(),
                            file.currentVersion(),
                            file.uploadedAt(),
                            file.deletedAt());
            files.put(id, saved);
            return saved;
        }

        @Override
        public Optional<SharedWorkspaceFile> findById(UUID fileId) {
            return Optional.ofNullable(files.get(fileId));
        }

        @Override
        public Optional<SharedWorkspaceFile> findByIdForUpdate(UUID fileId) {
            return findById(fileId);
        }

        @Override
        public List<SharedWorkspaceFile> findActiveByWorkspaceId(UUID workspaceId) {
            return files.values().stream()
                    .filter(file -> file.workspaceId().equals(workspaceId) && !file.isDeleted())
                    .toList();
        }
    }

    private static final class InMemoryVersionRepository
            implements SharedWorkspaceFileVersionRepositoryPort {
        private final List<SharedWorkspaceFileVersion> versions = new ArrayList<>();

        @Override
        public SharedWorkspaceFileVersion save(SharedWorkspaceFileVersion version) {
            versions.add(version);
            return version;
        }

        @Override
        public Optional<SharedWorkspaceFileVersion> findById(UUID versionId) {
            return Optional.empty();
        }

        @Override
        public List<SharedWorkspaceFileVersion> findByFileId(UUID fileId) {
            return versions.stream().filter(version -> version.fileId().equals(fileId)).toList();
        }
    }
}
