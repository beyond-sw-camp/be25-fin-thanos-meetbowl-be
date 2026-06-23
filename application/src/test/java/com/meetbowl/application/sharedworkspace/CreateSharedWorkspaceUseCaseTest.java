package com.meetbowl.application.sharedworkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeMemberRepository;
import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeWorkspaceRepository;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;

class CreateSharedWorkspaceUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-10T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void 생성하면_소유자_멤버가_같이_만들어진다() {
        FakeWorkspaceRepository workspaceRepository = new FakeWorkspaceRepository();
        FakeMemberRepository memberRepository = new FakeMemberRepository();
        CreateSharedWorkspaceUseCase useCase =
                new CreateSharedWorkspaceUseCase(
                        workspaceRepository, memberRepository, FIXED_CLOCK);

        UUID organizationId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        SharedWorkspaceResult result =
                useCase.execute(
                        new CreateSharedWorkspaceCommand(
                                organizationId, ownerUserId, "팀 자료실", "설명"));

        assertNotNull(result.workspaceId());
        assertEquals("MEMBERS_ONLY", result.visibility());

        SharedWorkspaceMember owner =
                memberRepository
                        .findByWorkspaceIdAndUserId(result.workspaceId(), ownerUserId)
                        .orElseThrow();
        assertTrue(owner.isOwner());
        assertTrue(owner.isActive());
    }
}
