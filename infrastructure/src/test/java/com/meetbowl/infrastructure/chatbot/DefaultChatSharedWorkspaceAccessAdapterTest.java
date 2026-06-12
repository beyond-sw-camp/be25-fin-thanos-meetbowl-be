package com.meetbowl.infrastructure.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.sharedworkspace.SharedWorkspace;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceVisibility;

class DefaultChatSharedWorkspaceAccessAdapterTest {

    private final SharedWorkspaceMemberRepositoryPort memberRepositoryPort =
            mock(SharedWorkspaceMemberRepositoryPort.class);
    private final SharedWorkspaceRepositoryPort workspaceRepositoryPort =
            mock(SharedWorkspaceRepositoryPort.class);
    private final DefaultChatSharedWorkspaceAccessAdapter adapter =
            new DefaultChatSharedWorkspaceAccessAdapter(
                    memberRepositoryPort, workspaceRepositoryPort);

    @Test
    void returnsOnlyActiveWorkspacesInCurrentOrganization() {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID accessibleId = UUID.randomUUID();
        UUID deletedId = UUID.randomUUID();
        UUID otherOrganizationId = UUID.randomUUID();

        when(memberRepositoryPort.findActiveByUserId(userId))
                .thenReturn(
                        List.of(
                                member(accessibleId, userId),
                                member(deletedId, userId),
                                member(otherOrganizationId, userId)));
        when(workspaceRepositoryPort.findById(accessibleId))
                .thenReturn(Optional.of(workspace(accessibleId, organizationId, null)));
        when(workspaceRepositoryPort.findById(deletedId))
                .thenReturn(
                        Optional.of(
                                workspace(
                                        deletedId,
                                        organizationId,
                                        Instant.parse("2026-06-11T00:00:00Z"))));
        when(workspaceRepositoryPort.findById(otherOrganizationId))
                .thenReturn(Optional.of(workspace(otherOrganizationId, UUID.randomUUID(), null)));

        assertThat(adapter.findAccessibleSharedWorkspaceIds(userId, organizationId))
                .isEqualTo(Set.of(accessibleId));
    }

    private SharedWorkspaceMember member(UUID workspaceId, UUID userId) {
        return SharedWorkspaceMember.owner(
                workspaceId, userId, Instant.parse("2026-06-01T00:00:00Z"));
    }

    private SharedWorkspace workspace(UUID workspaceId, UUID organizationId, Instant deletedAt) {
        return SharedWorkspace.of(
                workspaceId,
                organizationId,
                UUID.randomUUID(),
                "workspace",
                null,
                SharedWorkspaceVisibility.MEMBERS_ONLY,
                Instant.parse("2026-06-01T00:00:00Z"),
                deletedAt);
    }
}
