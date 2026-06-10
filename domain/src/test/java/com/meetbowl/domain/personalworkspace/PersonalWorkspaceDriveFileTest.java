package com.meetbowl.domain.personalworkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PersonalWorkspaceDriveFileTest {

    @Test
    void createDriveFileMetadata() {
        UUID ownerUserId = UUID.randomUUID();
        Instant uploadedAt = Instant.parse("2099-01-01T01:00:00Z");

        PersonalWorkspaceDriveFile file =
                PersonalWorkspaceDriveFile.create(
                        ownerUserId,
                        "회의자료.pdf",
                        "application/pdf",
                        1024L,
                        "personal-workspace/user/file.pdf",
                        uploadedAt);

        assertEquals(ownerUserId, file.ownerUserId());
        assertEquals("회의자료.pdf", file.originalFileName());
        assertEquals("personal-workspace/user/file.pdf", file.storageKey());
        assertFalse(file.isDeleted());

        PersonalWorkspaceDriveFile deleted = file.delete(Instant.parse("2099-01-02T01:00:00Z"));

        assertTrue(deleted.isDeleted());
    }
}
