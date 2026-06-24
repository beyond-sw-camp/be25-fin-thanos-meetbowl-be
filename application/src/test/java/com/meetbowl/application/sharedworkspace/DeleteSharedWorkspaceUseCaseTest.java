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
import com.meetbowl.application.sharedworkspace.SharedWorkspaceFakes.FakeWorkspaceRepository;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class DeleteSharedWorkspaceUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-24T00:00:00Z"), ZoneOffset.UTC);

    private FakeWorkspaceRepository workspaceRepository;
    private DeleteSharedWorkspaceUseCase useCase;
    private UUID workspaceId;
    private UUID ownerUserId;

    @BeforeEach
    void setUp() {
        workspaceRepository = new FakeWorkspaceRepository();
        FakeMemberRepository memberRepository = new FakeMemberRepository();
        SharedWorkspaceAccessGuard accessGuard =
                new SharedWorkspaceAccessGuard(workspaceRepository, memberRepository);
        useCase = new DeleteSharedWorkspaceUseCase(workspaceRepository, accessGuard, FIXED_CLOCK);

        ownerUserId = UUID.randomUUID();
        workspaceId =
                new CreateSharedWorkspaceUseCase(
                                workspaceRepository, memberRepository, FIXED_CLOCK)
                        .execute(
                                new CreateSharedWorkspaceCommand(
                                        UUID.randomUUID(), ownerUserId, "생성자 프로젝트", null))
                        .workspaceId();
    }

    @Test
    void 생성자는_프로젝트를_삭제할_수_있다() {
        useCase.execute(workspaceId, ownerUserId);

        assertTrue(workspaceRepository.findById(workspaceId).orElseThrow().isDeleted());
    }

    @Test
    void 생성자가_아니면_프로젝트를_삭제할_수_없다() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.execute(workspaceId, UUID.randomUUID()));

        assertEquals(ErrorCode.SHARED_WORKSPACE_FORBIDDEN, exception.errorCode());
    }
}
