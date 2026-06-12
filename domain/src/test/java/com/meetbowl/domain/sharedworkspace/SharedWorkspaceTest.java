package com.meetbowl.domain.sharedworkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class SharedWorkspaceTest {

    @Test
    void createWorkspaceAsMembersOnlyAndChangeVisibility() {
        UUID organizationId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();

        SharedWorkspace workspace =
                SharedWorkspace.create(
                        organizationId,
                        ownerUserId,
                        "제품팀 자료실",
                        "회의 자료 공유 공간",
                        Instant.parse("2099-01-01T01:00:00Z"));

        assertEquals(SharedWorkspaceVisibility.MEMBERS_ONLY, workspace.visibility());
        assertTrue(workspace.isOwnedBy(ownerUserId));
        assertFalse(workspace.isOrganizationVisible());

        SharedWorkspace organizationVisible = workspace.openToOrganization();

        assertEquals(SharedWorkspaceVisibility.ORGANIZATION, organizationVisible.visibility());
        assertTrue(organizationVisible.isOrganizationVisible());
    }

    @Test
    void ownerMemberCannotBeRemoved() {
        SharedWorkspaceMember owner =
                SharedWorkspaceMember.owner(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.parse("2099-01-01T01:00:00Z"));

        assertTrue(owner.isOwner());
        assertThrows(
                BusinessException.class, () -> owner.remove(Instant.parse("2099-01-02T01:00:00Z")));
    }

    @Test
    void addFileVersionUpdatesCurrentMetadata() {
        UUID uploaderUserId = UUID.randomUUID();
        SharedWorkspaceFile file =
                SharedWorkspaceFile.create(
                        UUID.randomUUID(),
                        uploaderUserId,
                        "기획서-v1.pdf",
                        "application/pdf",
                        1024L,
                        "shared/workspace/file-v1.pdf",
                        Instant.parse("2099-01-01T01:00:00Z"));

        SharedWorkspaceFile updated =
                file.addVersion(
                        uploaderUserId,
                        "기획서-v2.pdf",
                        "application/pdf",
                        2048L,
                        "shared/workspace/file-v2.pdf",
                        DocumentVersion.INITIAL,
                        DocumentVersion.parse("v.1.1.0"),
                        Instant.parse("2099-01-02T01:00:00Z"));

        assertEquals(DocumentVersion.parse("1.1.0"), updated.currentVersion());
        assertEquals("기획서-v2.pdf", updated.originalFileName());
        assertEquals("shared/workspace/file-v2.pdf", updated.storageKey());
    }

    @Test
    void updateFileVersionChangeMemo() {
        SharedWorkspaceFileVersion version =
                SharedWorkspaceFileVersion.create(
                        UUID.randomUUID(),
                        DocumentVersion.INITIAL,
                        UUID.randomUUID(),
                        "기획서.pdf",
                        "application/pdf",
                        1024L,
                        "shared/workspace/file-v1.pdf",
                        null,
                        Instant.parse("2099-01-01T01:00:00Z"));

        SharedWorkspaceFileVersion updated = version.updateChangeMemo("초안 업로드");

        assertEquals("초안 업로드", updated.changeMemo());
    }

    @Test
    void createFileStartsAtVersionOne() {
        SharedWorkspaceFile file =
                SharedWorkspaceFile.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "기획서.pdf",
                        "application/pdf",
                        1024L,
                        "shared/workspace/file.pdf",
                        Instant.parse("2099-01-01T01:00:00Z"));

        assertEquals("v.1.0.0", file.currentVersion().displayValue());
    }

    @Test
    void rejectSameOlderAndStaleFileVersions() {
        UUID uploaderUserId = UUID.randomUUID();
        SharedWorkspaceFile file =
                SharedWorkspaceFile.create(
                        UUID.randomUUID(),
                        uploaderUserId,
                        "기획서.pdf",
                        "application/pdf",
                        1024L,
                        "shared/workspace/file.pdf",
                        Instant.parse("2099-01-01T01:00:00Z"));

        assertThrows(
                BusinessException.class,
                () ->
                        file.addVersion(
                                uploaderUserId,
                                "기획서.pdf",
                                "application/pdf",
                                1024L,
                                "shared/workspace/file.pdf",
                                DocumentVersion.INITIAL,
                                DocumentVersion.INITIAL,
                                Instant.parse("2099-01-02T01:00:00Z")));
        assertThrows(
                BusinessException.class,
                () ->
                        file.addVersion(
                                uploaderUserId,
                                "기획서.pdf",
                                "application/pdf",
                                1024L,
                                "shared/workspace/file.pdf",
                                DocumentVersion.parse("1.0.1"),
                                DocumentVersion.parse("1.1.0"),
                                Instant.parse("2099-01-02T01:00:00Z")));
    }

    @Test
    void removedMemberCanBeInvitedAgain() {
        UUID inviterUserId = UUID.randomUUID();
        SharedWorkspaceMember removed =
                SharedWorkspaceMember.invite(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                inviterUserId,
                                Instant.parse("2099-01-01T01:00:00Z"))
                        .remove(Instant.parse("2099-01-02T01:00:00Z"));

        SharedWorkspaceMember reactivated =
                removed.reactivate(inviterUserId, Instant.parse("2099-01-03T01:00:00Z"));

        assertTrue(reactivated.isActive());
        assertEquals(null, reactivated.removedAt());
    }
}
