package com.meetbowl.application.sharedworkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeMemberRepository;
import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeUserRepository;
import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeWorkspaceRepository;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;
import com.meetbowl.domain.user.UserStatus;

class InviteSharedWorkspaceMemberUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-10T00:00:00Z"), ZoneOffset.UTC);

    private FakeWorkspaceRepository workspaceRepository;
    private FakeMemberRepository memberRepository;
    private FakeUserRepository userRepository;
    private InviteSharedWorkspaceMemberUseCase useCase;

    private UUID workspaceId;
    private UUID ownerUserId;

    @BeforeEach
    void setUp() {
        workspaceRepository = new FakeWorkspaceRepository();
        memberRepository = new FakeMemberRepository();
        userRepository = new FakeUserRepository();
        SharedWorkspaceAccessGuard accessGuard =
                new SharedWorkspaceAccessGuard(workspaceRepository, memberRepository);
        useCase =
                new InviteSharedWorkspaceMemberUseCase(
                        memberRepository, userRepository, accessGuard, FIXED_CLOCK);

        ownerUserId = UUID.randomUUID();
        SharedWorkspaceResult workspace =
                new CreateSharedWorkspaceUseCase(workspaceRepository, memberRepository, FIXED_CLOCK)
                        .execute(
                                new CreateSharedWorkspaceCommand(
                                        UUID.randomUUID(), ownerUserId, "팀 자료실", null));
        workspaceId = workspace.workspaceId();
    }

    @Test
    void 소유자가_재직중_사용자를_초대하면_멤버가_된다() {
        UUID inviteeId = UUID.randomUUID();
        userRepository.put(inviteeId, UserStatus.ACTIVE);

        SharedWorkspaceMemberResult result =
                useCase.execute(
                        new InviteSharedWorkspaceMemberCommand(
                                workspaceId, ownerUserId, inviteeId));

        assertEquals("ACTIVE", result.status());
        assertEquals("MEMBER", result.role());
    }

    @Test
    void 탈퇴했던_멤버는_재활성화로_다시_초대된다() {
        UUID inviteeId = UUID.randomUUID();
        userRepository.put(inviteeId, UserStatus.ACTIVE);
        useCase.execute(
                new InviteSharedWorkspaceMemberCommand(workspaceId, ownerUserId, inviteeId));
        SharedWorkspaceMember member =
                memberRepository.findByWorkspaceIdAndUserId(workspaceId, inviteeId).orElseThrow();
        memberRepository.save(member.remove(Instant.parse("2026-06-10T01:00:00Z")));

        useCase.execute(
                new InviteSharedWorkspaceMemberCommand(workspaceId, ownerUserId, inviteeId));

        SharedWorkspaceMember reactivated =
                memberRepository.findByWorkspaceIdAndUserId(workspaceId, inviteeId).orElseThrow();
        assertTrue(reactivated.isActive());
        // 멤버 행이 새로 늘지 않고 기존 행이 재활성화되므로 소유자+초대자 2명만 활성으로 남는다.
        assertEquals(2, memberRepository.findActiveByWorkspaceId(workspaceId).size());
    }

    @Test
    void 퇴사한_사용자는_초대할_수_없다() {
        UUID inviteeId = UUID.randomUUID();
        userRepository.put(inviteeId, UserStatus.INACTIVE);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new InviteSharedWorkspaceMemberCommand(
                                                workspaceId, ownerUserId, inviteeId)));
        assertEquals(ErrorCode.SHARED_WORKSPACE_MEMBER_RESIGNED, exception.errorCode());
    }

    @Test
    void 소유자가_아니면_초대할_수_없다() {
        UUID inviteeId = UUID.randomUUID();
        userRepository.put(inviteeId, UserStatus.ACTIVE);
        UUID strangerId = UUID.randomUUID();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.execute(
                                        new InviteSharedWorkspaceMemberCommand(
                                                workspaceId, strangerId, inviteeId)));
        assertEquals(ErrorCode.SHARED_WORKSPACE_FORBIDDEN, exception.errorCode());
    }
}
