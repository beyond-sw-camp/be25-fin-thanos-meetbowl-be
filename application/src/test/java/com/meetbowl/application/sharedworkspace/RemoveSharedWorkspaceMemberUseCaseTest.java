package com.meetbowl.application.sharedworkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;

class RemoveSharedWorkspaceMemberUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-10T00:00:00Z"), ZoneOffset.UTC);

    private FakeWorkspaceRepository workspaceRepository;
    private FakeMemberRepository memberRepository;
    private RemoveSharedWorkspaceMemberUseCase useCase;

    private UUID workspaceId;
    private UUID ownerUserId;
    private UUID memberUserId;

    @BeforeEach
    void setUp() {
        workspaceRepository = new FakeWorkspaceRepository();
        memberRepository = new FakeMemberRepository();
        SharedWorkspaceAccessGuard accessGuard =
                new SharedWorkspaceAccessGuard(workspaceRepository, memberRepository);
        useCase =
                new RemoveSharedWorkspaceMemberUseCase(memberRepository, accessGuard, FIXED_CLOCK);

        ownerUserId = UUID.randomUUID();
        SharedWorkspaceResult workspace =
                new CreateSharedWorkspaceUseCase(workspaceRepository, memberRepository, FIXED_CLOCK)
                        .execute(
                                new CreateSharedWorkspaceCommand(
                                        UUID.randomUUID(), ownerUserId, "팀 자료실", null));
        workspaceId = workspace.workspaceId();

        memberUserId = UUID.randomUUID();
        memberRepository.save(
                SharedWorkspaceMember.invite(
                        workspaceId, memberUserId, ownerUserId, FIXED_CLOCK.instant()));
    }

    @Test
    void 소유자는_멤버를_추방할_수_있다() {
        useCase.execute(workspaceId, ownerUserId, memberUserId);

        SharedWorkspaceMember member =
                memberRepository
                        .findByWorkspaceIdAndUserId(workspaceId, memberUserId)
                        .orElseThrow();
        assertFalse(member.isActive());
    }

    @Test
    void 본인은_스스로_탈퇴할_수_있다() {
        useCase.execute(workspaceId, memberUserId, memberUserId);

        SharedWorkspaceMember member =
                memberRepository
                        .findByWorkspaceIdAndUserId(workspaceId, memberUserId)
                        .orElseThrow();
        assertFalse(member.isActive());
    }

    @Test
    void 소유자가_아닌_사람은_다른_멤버를_추방할_수_없다() {
        UUID strangerId = UUID.randomUUID();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(workspaceId, strangerId, memberUserId));
        assertEquals(ErrorCode.SHARED_WORKSPACE_FORBIDDEN, exception.errorCode());
    }
}
