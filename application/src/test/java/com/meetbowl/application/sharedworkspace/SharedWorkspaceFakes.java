package com.meetbowl.application.sharedworkspace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.meetbowl.domain.common.Paged;
import com.meetbowl.domain.sharedworkspace.SharedWorkspace;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersionRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberStatus;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

/**
 * 공유 워크스페이스 UseCase 테스트용 in-memory Fake 모음이다. 저장 시 id가 비어 있으면 새 UUID를 부여해 실제 Persistence Adapter가
 * PK를 발급하는 동작을 흉내 낸다. 테스트가 권한·버전 규칙 같은 비즈니스 분기에 집중하도록 검증 로직은 두지 않는다.
 */
final class SharedWorkspaceFakes {

    private SharedWorkspaceFakes() {}

    static final class FakeWorkspaceRepository implements SharedWorkspaceRepositoryPort {

        final Map<UUID, SharedWorkspace> store = new LinkedHashMap<>();

        @Override
        public SharedWorkspace save(SharedWorkspace workspace) {
            UUID id = workspace.id() != null ? workspace.id() : UUID.randomUUID();
            SharedWorkspace stored =
                    SharedWorkspace.of(
                            id,
                            workspace.organizationId(),
                            workspace.ownerUserId(),
                            workspace.name(),
                            workspace.description(),
                            workspace.visibility(),
                            workspace.createdAt(),
                            workspace.deletedAt());
            store.put(id, stored);
            return stored;
        }

        @Override
        public Optional<SharedWorkspace> findById(UUID workspaceId) {
            return Optional.ofNullable(store.get(workspaceId));
        }

        @Override
        public List<SharedWorkspace> findActiveByOwnerUserId(UUID ownerUserId) {
            List<SharedWorkspace> result = new ArrayList<>();
            for (SharedWorkspace workspace : store.values()) {
                if (!workspace.isDeleted() && workspace.isOwnedBy(ownerUserId)) {
                    result.add(workspace);
                }
            }
            return result;
        }

        @Override
        public List<SharedWorkspace> findOrganizationVisible(UUID organizationId) {
            List<SharedWorkspace> result = new ArrayList<>();
            for (SharedWorkspace workspace : store.values()) {
                if (!workspace.isDeleted()
                        && workspace.isOrganizationVisible()
                        && workspace.organizationId().equals(organizationId)) {
                    result.add(workspace);
                }
            }
            return result;
        }
    }

    static final class FakeMemberRepository implements SharedWorkspaceMemberRepositoryPort {

        final Map<UUID, SharedWorkspaceMember> store = new LinkedHashMap<>();

        @Override
        public SharedWorkspaceMember save(SharedWorkspaceMember member) {
            UUID id = member.id() != null ? member.id() : UUID.randomUUID();
            SharedWorkspaceMember stored =
                    SharedWorkspaceMember.of(
                            id,
                            member.workspaceId(),
                            member.userId(),
                            member.role(),
                            member.status(),
                            member.invitedByUserId(),
                            member.joinedAt(),
                            member.removedAt());
            store.put(id, stored);
            return stored;
        }

        @Override
        public Optional<SharedWorkspaceMember> findByWorkspaceIdAndUserId(
                UUID workspaceId, UUID userId) {
            return store.values().stream()
                    .filter(
                            member ->
                                    member.workspaceId().equals(workspaceId)
                                            && member.userId().equals(userId))
                    .findFirst();
        }

        @Override
        public List<SharedWorkspaceMember> findActiveByWorkspaceId(UUID workspaceId) {
            return store.values().stream()
                    .filter(member -> member.workspaceId().equals(workspaceId))
                    .filter(member -> member.status() == SharedWorkspaceMemberStatus.ACTIVE)
                    .toList();
        }

        @Override
        public List<SharedWorkspaceMember> findActiveByUserId(UUID userId) {
            return store.values().stream()
                    .filter(member -> member.userId().equals(userId))
                    .filter(member -> member.status() == SharedWorkspaceMemberStatus.ACTIVE)
                    .toList();
        }
    }

    static final class FakeFileRepository implements SharedWorkspaceFileRepositoryPort {

        final Map<UUID, SharedWorkspaceFile> store = new LinkedHashMap<>();

        @Override
        public SharedWorkspaceFile save(SharedWorkspaceFile file) {
            UUID id = file.id() != null ? file.id() : UUID.randomUUID();
            SharedWorkspaceFile stored =
                    SharedWorkspaceFile.of(
                            id,
                            file.workspaceId(),
                            file.uploaderUserId(),
                            file.originalFileName(),
                            file.contentType(),
                            file.sizeBytes(),
                            file.storageKey(),
                            file.currentVersion(),
                            file.uploadedAt(),
                            file.deletedAt());
            store.put(id, stored);
            return stored;
        }

        @Override
        public Optional<SharedWorkspaceFile> findById(UUID fileId) {
            return Optional.ofNullable(store.get(fileId));
        }

        @Override
        public Optional<SharedWorkspaceFile> findByIdForUpdate(UUID fileId) {
            return findById(fileId);
        }

        @Override
        public List<SharedWorkspaceFile> findActiveByWorkspaceId(UUID workspaceId) {
            return store.values().stream()
                    .filter(file -> file.workspaceId().equals(workspaceId))
                    .filter(file -> !file.isDeleted())
                    .toList();
        }
    }

    static final class FakeFileVersionRepository
            implements SharedWorkspaceFileVersionRepositoryPort {

        final Map<UUID, SharedWorkspaceFileVersion> store = new LinkedHashMap<>();

        @Override
        public SharedWorkspaceFileVersion save(SharedWorkspaceFileVersion version) {
            UUID id = version.id() != null ? version.id() : UUID.randomUUID();
            SharedWorkspaceFileVersion stored =
                    SharedWorkspaceFileVersion.of(
                            id,
                            version.fileId(),
                            version.version(),
                            version.uploaderUserId(),
                            version.originalFileName(),
                            version.contentType(),
                            version.sizeBytes(),
                            version.storageKey(),
                            version.changeMemo(),
                            version.uploadedAt());
            store.put(id, stored);
            return stored;
        }

        @Override
        public Optional<SharedWorkspaceFileVersion> findById(UUID versionId) {
            return Optional.ofNullable(store.get(versionId));
        }

        @Override
        public List<SharedWorkspaceFileVersion> findByFileId(UUID fileId) {
            return store.values().stream()
                    .filter(version -> version.fileId().equals(fileId))
                    .toList();
        }
    }

    static final class FakeUserRepository implements UserRepositoryPort {

        final Map<UUID, User> store = new LinkedHashMap<>();

        void put(UUID userId, UserStatus status) {
            Instant now = Instant.parse("2026-01-01T00:00:00Z");
            store.put(
                    userId,
                    User.of(
                            userId,
                            "user-" + userId,
                            "hash",
                            "사용자",
                            userId + "@meetbowl.test",
                            UserRole.USER,
                            status,
                            null,
                            null,
                            null,
                            null,
                            false,
                            null,
                            null,
                            now,
                            now));
        }

        @Override
        public User save(User user) {
            store.put(user.id(), user);
            return user;
        }

        @Override
        public Optional<User> findById(UUID userId) {
            return Optional.ofNullable(store.get(userId));
        }

        @Override
        public Optional<User> findByLoginId(String loginId) {
            return store.values().stream()
                    .filter(user -> user.loginId().equals(loginId))
                    .findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return store.values().stream().filter(user -> user.email().equals(email)).findFirst();
        }

        @Override
        public boolean existsByLoginId(String loginId) {
            return findByLoginId(loginId).isPresent();
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public Paged<User> findPage(String keyword, int page, int size) {
            return new Paged<>(List.copyOf(store.values()), store.size());
        }

        @Override
        public Paged<User> search(
                String keyword,
                UUID affiliateId,
                UUID departmentId,
                UUID teamId,
                UUID positionId,
                UserStatus status,
                int page,
                int size) {
            return new Paged<>(List.copyOf(store.values()), store.size());
        }

        @Override
        public java.util.List<User> findAllByAffiliateId(UUID affiliateId) {
            return store.values().stream()
                    .filter(user -> affiliateId.equals(user.affiliateId()))
                    .toList();
        }

        @Override
        public java.util.List<User> findAllByDepartmentId(UUID departmentId) {
            return store.values().stream()
                    .filter(user -> departmentId.equals(user.departmentId()))
                    .toList();
        }

        @Override
        public java.util.List<User> findAllByTeamId(UUID teamId) {
            return store.values().stream().filter(user -> teamId.equals(user.teamId())).toList();
        }

        @Override
        public java.util.List<User> findAllByPositionId(UUID positionId) {
            return store.values().stream()
                    .filter(user -> positionId.equals(user.positionId()))
                    .toList();
        }
    }
}
