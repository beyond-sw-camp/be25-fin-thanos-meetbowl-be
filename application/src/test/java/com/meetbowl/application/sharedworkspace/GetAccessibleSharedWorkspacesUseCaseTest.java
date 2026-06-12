package com.meetbowl.application.sharedworkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeMemberRepository;
import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeWorkspaceRepository;

class GetAccessibleSharedWorkspacesUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-10T00:00:00Z"), ZoneOffset.UTC);

    private FakeWorkspaceRepository workspaceRepository;
    private FakeMemberRepository memberRepository;
    private CreateSharedWorkspaceUseCase createUseCase;
    private ChangeSharedWorkspaceAudienceUseCase audienceUseCase;
    private GetAccessibleSharedWorkspacesUseCase useCase;

    @BeforeEach
    void setUp() {
        workspaceRepository = new FakeWorkspaceRepository();
        memberRepository = new FakeMemberRepository();
        SharedWorkspaceAccessGuard accessGuard =
                new SharedWorkspaceAccessGuard(workspaceRepository, memberRepository);
        createUseCase =
                new CreateSharedWorkspaceUseCase(
                        workspaceRepository, memberRepository, FIXED_CLOCK);
        audienceUseCase =
                new ChangeSharedWorkspaceAudienceUseCase(workspaceRepository, accessGuard);
        useCase = new GetAccessibleSharedWorkspacesUseCase(workspaceRepository, memberRepository);
    }

    @Test
    void 멤버인_워크스페이스와_전직원_공개_워크스페이스가_중복없이_조회된다() {
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // 사용자가 소유(=멤버)한 워크스페이스
        SharedWorkspaceResult owned =
                createUseCase.execute(
                        new CreateSharedWorkspaceCommand(organizationId, userId, "내 자료실", null));

        // 같은 조직의 다른 사람이 만든 전직원 공개 워크스페이스
        UUID otherOwner = UUID.randomUUID();
        SharedWorkspaceResult orgVisible =
                createUseCase.execute(
                        new CreateSharedWorkspaceCommand(
                                organizationId, otherOwner, "공지 자료실", null));
        audienceUseCase.execute(
                new ChangeSharedWorkspaceAudienceCommand(
                        orgVisible.workspaceId(), otherOwner, false, true));

        // 다른 조직의 전직원 공개 워크스페이스는 보이면 안 된다
        createUseCase.execute(
                new CreateSharedWorkspaceCommand(UUID.randomUUID(), otherOwner, "타조직 자료실", null));

        List<SharedWorkspaceResult> result = useCase.execute(userId, organizationId);

        Set<UUID> ids =
                result.stream().map(SharedWorkspaceResult::workspaceId).collect(Collectors.toSet());
        assertEquals(2, ids.size());
        assertTrue(ids.contains(owned.workspaceId()));
        assertTrue(ids.contains(orgVisible.workspaceId()));
    }
}
