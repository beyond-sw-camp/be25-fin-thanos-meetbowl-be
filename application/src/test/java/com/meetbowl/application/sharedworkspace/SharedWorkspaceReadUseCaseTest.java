package com.meetbowl.application.sharedworkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeFileRepository;
import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeFileVersionRepository;
import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeMemberRepository;
import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeWorkspaceRepository;
import com.meetbowl.domain.document.DocumentIndexRequestedEventPort;
import com.meetbowl.domain.storage.ObjectStoragePort;
import com.meetbowl.domain.storage.StoredObject;

class SharedWorkspaceReadUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-10T00:00:00Z"), ZoneOffset.UTC);
    private static final byte[] PDF_BYTES = "pdf-content".getBytes();

    private FakeWorkspaceRepository workspaceRepository;
    private FakeMemberRepository memberRepository;
    private FakeFileRepository fileRepository;
    private FakeFileVersionRepository versionRepository;
    private SharedWorkspaceAccessGuard accessGuard;
    private ObjectStoragePort objectStoragePort;

    private UUID organizationId;
    private UUID workspaceId;
    private UUID fileId;
    private UUID sameOrganizationNonMemberId;

    @BeforeEach
    void setUp() {
        workspaceRepository = new FakeWorkspaceRepository();
        memberRepository = new FakeMemberRepository();
        fileRepository = new FakeFileRepository();
        versionRepository = new FakeFileVersionRepository();
        accessGuard = new SharedWorkspaceAccessGuard(workspaceRepository, memberRepository);
        objectStoragePort = mock(ObjectStoragePort.class);
        DocumentIndexRequestedEventPort indexEventPort = mock(DocumentIndexRequestedEventPort.class);

        organizationId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        sameOrganizationNonMemberId = UUID.randomUUID();

        SharedWorkspaceResult workspace =
                new CreateSharedWorkspaceUseCase(workspaceRepository, memberRepository, FIXED_CLOCK)
                        .execute(
                                new CreateSharedWorkspaceCommand(
                                        organizationId, ownerUserId, "전사 자료실", null));
        workspaceId = workspace.workspaceId();
        new ChangeSharedWorkspaceAudienceUseCase(workspaceRepository, accessGuard)
                .execute(
                        new ChangeSharedWorkspaceAudienceCommand(
                                workspaceId, ownerUserId, false, true));

        SharedWorkspaceFileResult file =
                new UploadSharedWorkspaceFileUseCase(
                                fileRepository,
                                versionRepository,
                                accessGuard,
                                objectStoragePort,
                                indexEventPort,
                                FIXED_CLOCK)
                        .execute(
                                new UploadSharedWorkspaceFileCommand(
                                        workspaceId,
                                        ownerUserId,
                                        organizationId,
                                        "policy.pdf",
                                        PDF_BYTES));
        fileId = file.fileId();
    }

    @Test
    void 전직원_공개_워크스페이스의_파일_목록과_상세를_비멤버도_읽을_수_있다() {
        List<SharedWorkspaceFileResult> files =
                new GetSharedWorkspaceFilesUseCase(fileRepository, accessGuard)
                        .execute(workspaceId, sameOrganizationNonMemberId, organizationId);

        SharedWorkspaceFileResult detail =
                new GetSharedWorkspaceFileUseCase(fileRepository, accessGuard)
                        .execute(
                                workspaceId,
                                fileId,
                                sameOrganizationNonMemberId,
                                organizationId);

        assertEquals(
                List.of(fileId), files.stream().map(SharedWorkspaceFileResult::fileId).toList());
        assertEquals(fileId, detail.fileId());
    }

    @Test
    void 전직원_공개_워크스페이스의_파일_다운로드와_버전_목록을_비멤버도_읽을_수_있다() {
        when(objectStoragePort.download(anyString()))
                .thenReturn(
                        new StoredObject(
                                new ByteArrayInputStream(PDF_BYTES),
                                "application/pdf",
                                PDF_BYTES.length));

        SharedWorkspaceFileDownloadResult download =
                new DownloadSharedWorkspaceFileUseCase(
                                fileRepository, accessGuard, objectStoragePort)
                        .execute(
                                workspaceId,
                                fileId,
                                sameOrganizationNonMemberId,
                                organizationId);
        List<SharedWorkspaceFileVersionResult> versions =
                new GetSharedWorkspaceFileVersionsUseCase(
                                fileRepository, versionRepository, accessGuard)
                        .execute(
                                workspaceId,
                                fileId,
                                sameOrganizationNonMemberId,
                                organizationId);

        assertEquals(fileId, download.fileId());
        assertNotNull(download.content());
        assertEquals(
                List.of("v.1.0.0"),
                versions.stream().map(SharedWorkspaceFileVersionResult::version).toList());
    }
}
