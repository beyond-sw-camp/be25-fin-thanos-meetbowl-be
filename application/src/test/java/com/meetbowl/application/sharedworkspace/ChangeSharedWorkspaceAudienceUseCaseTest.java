package com.meetbowl.application.sharedworkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeMemberRepository;
import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeWorkspaceRepository;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class ChangeSharedWorkspaceAudienceUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-10T00:00:00Z"), ZoneOffset.UTC);

    private FakeWorkspaceRepository workspaceRepository;
    private FakeMemberRepository memberRepository;
    private ChangeSharedWorkspaceAudienceUseCase useCase;

    private UUID workspaceId;
    private UUID ownerUserId;

    @BeforeEach
    void setUp() {
        workspaceRepository = new FakeWorkspaceRepository();
        memberRepository = new FakeMemberRepository();
        SharedWorkspaceAccessGuard accessGuard =
                new SharedWorkspaceAccessGuard(workspaceRepository, memberRepository);
        useCase = new ChangeSharedWorkspaceAudienceUseCase(workspaceRepository, accessGuard);

        ownerUserId = UUID.randomUUID();
        SharedWorkspaceResult workspace =
                new CreateSharedWorkspaceUseCase(workspaceRepository, memberRepository, FIXED_CLOCK)
                        .execute(
                                new CreateSharedWorkspaceCommand(
                                        UUID.randomUUID(), ownerUserId, "팀 자료실", null));
        workspaceId = workspace.workspaceId();
    }

    @Test
    void 소유자는_전직원_공개로_전환할_수_있다() {
        SharedWorkspaceResult result =
                useCase.execute(
                        new ChangeSharedWorkspaceAudienceCommand(
                                workspaceId, ownerUserId, false, true));
        assertEquals("ORGANIZATION", result.visibility());
    }

    @Test
    void 관리자는_소유자가_아니어도_공개_범위를_바꿀_수_있다() {
        UUID adminId = UUID.randomUUID();
        SharedWorkspaceResult result =
                useCase.execute(
                        new ChangeSharedWorkspaceAudienceCommand(workspaceId, adminId, true, true));
        assertEquals("ORGANIZATION", result.visibility());
    }

    @Test
    void 소유자도_관리자도_아니면_거부된다() {
        UUID strangerId = UUID.randomUUID();
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new ChangeSharedWorkspaceAudienceCommand(
                                                workspaceId, strangerId, false, true)));
        assertEquals(ErrorCode.SHARED_WORKSPACE_FORBIDDEN, exception.errorCode());
    }
}
