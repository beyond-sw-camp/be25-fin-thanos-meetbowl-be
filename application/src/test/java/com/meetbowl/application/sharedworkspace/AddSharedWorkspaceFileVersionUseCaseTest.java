package com.meetbowl.application.sharedworkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeFileRepository;
import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeFileVersionRepository;
import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeMemberRepository;
import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeWorkspaceRepository;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class AddSharedWorkspaceFileVersionUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-10T00:00:00Z"), ZoneOffset.UTC);

    private FakeWorkspaceRepository workspaceRepository;
    private FakeMemberRepository memberRepository;
    private FakeFileRepository fileRepository;
    private FakeFileVersionRepository versionRepository;
    private AddSharedWorkspaceFileVersionUseCase useCase;

    private UUID workspaceId;
    private UUID ownerUserId;
    private UUID fileId;

    @BeforeEach
    void setUp() {
        workspaceRepository = new FakeWorkspaceRepository();
        memberRepository = new FakeMemberRepository();
        fileRepository = new FakeFileRepository();
        versionRepository = new FakeFileVersionRepository();
        SharedWorkspaceAccessGuard accessGuard =
                new SharedWorkspaceAccessGuard(workspaceRepository, memberRepository);
        useCase =
                new AddSharedWorkspaceFileVersionUseCase(
                        fileRepository, versionRepository, accessGuard, FIXED_CLOCK);

        ownerUserId = UUID.randomUUID();
        SharedWorkspaceResult workspace =
                new CreateSharedWorkspaceUseCase(workspaceRepository, memberRepository, FIXED_CLOCK)
                        .execute(
                                new CreateSharedWorkspaceCommand(
                                        UUID.randomUUID(), ownerUserId, "팀 자료실", null));
        workspaceId = workspace.workspaceId();

        SharedWorkspaceFileResult file =
                new UploadSharedWorkspaceFileUseCase(
                                fileRepository, versionRepository, accessGuard, FIXED_CLOCK)
                        .execute(
                                new UploadSharedWorkspaceFileCommand(
                                        workspaceId,
                                        ownerUserId,
                                        "spec.pdf",
                                        "application/pdf",
                                        1024,
                                        "s3://bucket/spec-v1"));
        fileId = file.fileId();
    }

    @Test
    void 기대버전이_맞으면_새_버전이_올라간다() {
        SharedWorkspaceFileVersionResult result =
                useCase.execute(
                        new AddSharedWorkspaceFileVersionCommand(
                                workspaceId,
                                fileId,
                                ownerUserId,
                                "spec.pdf",
                                "application/pdf",
                                2048,
                                "s3://bucket/spec-v2",
                                "v.1.0.0",
                                "v.1.1.0",
                                "검토 의견 반영"));

        assertEquals("v.1.1.0", result.version());
        assertEquals(
                "v.1.1.0",
                fileRepository.findById(fileId).orElseThrow().currentVersion().displayValue());
    }

    @Test
    void 기대버전이_현재와_다르면_충돌이다() {
        // 먼저 1.1.0으로 올려 현재 버전을 바꾼 뒤, 여전히 1.0.0을 기대하는 요청을 보낸다.
        useCase.execute(
                new AddSharedWorkspaceFileVersionCommand(
                        workspaceId,
                        fileId,
                        ownerUserId,
                        "spec.pdf",
                        "application/pdf",
                        2048,
                        "s3://bucket/spec-v2",
                        "v.1.0.0",
                        "v.1.1.0",
                        null));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new AddSharedWorkspaceFileVersionCommand(
                                                workspaceId,
                                                fileId,
                                                ownerUserId,
                                                "spec.pdf",
                                                "application/pdf",
                                                4096,
                                                "s3://bucket/spec-v3",
                                                "v.1.0.0",
                                                "v.1.2.0",
                                                null)));
        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void 멤버가_아니면_버전을_올릴_수_없다() {
        UUID strangerId = UUID.randomUUID();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new AddSharedWorkspaceFileVersionCommand(
                                                workspaceId,
                                                fileId,
                                                strangerId,
                                                "spec.pdf",
                                                "application/pdf",
                                                2048,
                                                "s3://bucket/spec-v2",
                                                "v.1.0.0",
                                                "v.1.1.0",
                                                null)));
        assertEquals(ErrorCode.SHARED_WORKSPACE_FORBIDDEN, exception.errorCode());
    }
}
