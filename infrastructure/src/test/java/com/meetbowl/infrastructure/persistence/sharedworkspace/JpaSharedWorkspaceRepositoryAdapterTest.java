package com.meetbowl.infrastructure.persistence.sharedworkspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.meetbowl.domain.sharedworkspace.DocumentVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspace;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;
import com.meetbowl.infrastructure.config.InfrastructureConfig;

@SpringBootTest(classes = JpaSharedWorkspaceRepositoryAdapterTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:shared-workspace-jpa-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class JpaSharedWorkspaceRepositoryAdapterTest {

    @Autowired private JpaSharedWorkspaceRepositoryAdapter workspaceAdapter;
    @Autowired private JpaSharedWorkspaceMemberRepositoryAdapter memberAdapter;
    @Autowired private JpaSharedWorkspaceFileRepositoryAdapter fileAdapter;
    @Autowired private JpaSharedWorkspaceFileVersionRepositoryAdapter versionAdapter;

    @Test
    void saveWorkspaceMembersFilesAndVersions() {
        UUID organizationId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();
        Instant now = Instant.parse("2099-01-01T01:00:00Z");

        SharedWorkspace workspace =
                workspaceAdapter.save(
                        SharedWorkspace.create(organizationId, ownerUserId, "제품팀 자료실", "공유 자료", now)
                                .openToOrganization());
        SharedWorkspaceMember owner =
                memberAdapter.save(SharedWorkspaceMember.owner(workspace.id(), ownerUserId, now));
        SharedWorkspaceMember member =
                memberAdapter.save(
                        SharedWorkspaceMember.invite(
                                workspace.id(), memberUserId, ownerUserId, now.plusSeconds(60)));
        SharedWorkspaceFile file =
                fileAdapter.save(
                        SharedWorkspaceFile.create(
                                workspace.id(),
                                memberUserId,
                                "기획서-v1.pdf",
                                "application/pdf",
                                1024L,
                                "shared/workspace/file-v1.pdf",
                                now.plusSeconds(120)));
        SharedWorkspaceFileVersion firstVersion =
                versionAdapter.save(
                        SharedWorkspaceFileVersion.create(
                                file.id(),
                                DocumentVersion.INITIAL,
                                memberUserId,
                                "기획서-v1.pdf",
                                "application/pdf",
                                1024L,
                                "shared/workspace/file-v1.pdf",
                                "초안",
                                now.plusSeconds(120)));
        SharedWorkspaceFile updatedFile =
                fileAdapter.save(
                        file.addVersion(
                                memberUserId,
                                "기획서-v2.pdf",
                                "application/pdf",
                                2048L,
                                "shared/workspace/file-v2.pdf",
                                DocumentVersion.INITIAL,
                                DocumentVersion.parse("1.1.0"),
                                now.plusSeconds(180)));
        SharedWorkspaceFileVersion secondVersion =
                versionAdapter.save(
                        SharedWorkspaceFileVersion.create(
                                file.id(),
                                DocumentVersion.parse("1.1.0"),
                                memberUserId,
                                "기획서-v2.pdf",
                                "application/pdf",
                                2048L,
                                "shared/workspace/file-v2.pdf",
                                "피드백 반영",
                                now.plusSeconds(180)));

        List<SharedWorkspace> visibleWorkspaces =
                workspaceAdapter.findOrganizationVisible(organizationId);
        List<SharedWorkspaceMember> members = memberAdapter.findActiveByWorkspaceId(workspace.id());
        List<SharedWorkspaceFile> files = fileAdapter.findActiveByWorkspaceId(workspace.id());
        List<SharedWorkspaceFileVersion> versions = versionAdapter.findByFileId(file.id());

        assertThat(workspace.id()).isNotNull();
        assertThat(owner.isOwner()).isTrue();
        assertThat(member.isActive()).isTrue();
        assertThat(visibleWorkspaces).hasSize(1);
        assertThat(files).hasSize(1);
        assertThat(updatedFile.currentVersion()).isEqualTo(DocumentVersion.parse("1.1.0"));
        assertThat(firstVersion.id()).isNotNull();
        assertThat(secondVersion.id()).isNotNull();
        assertThat(versions)
                .extracting(SharedWorkspaceFileVersion::version)
                .containsExactly(DocumentVersion.parse("1.1.0"), DocumentVersion.INITIAL);
        assertThat(members)
                .extracting(SharedWorkspaceMember::userId)
                .contains(ownerUserId, memberUserId);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        SharedWorkspaceJpaConfig.class,
        JpaSharedWorkspaceRepositoryAdapter.class,
        JpaSharedWorkspaceMemberRepositoryAdapter.class,
        JpaSharedWorkspaceFileRepositoryAdapter.class,
        JpaSharedWorkspaceFileVersionRepositoryAdapter.class
    })
    static class TestApplication {}
}
