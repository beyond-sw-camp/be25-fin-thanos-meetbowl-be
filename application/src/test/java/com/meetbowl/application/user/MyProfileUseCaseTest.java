package com.meetbowl.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.common.Paged;
import com.meetbowl.domain.organization.Affiliate;
import com.meetbowl.domain.organization.AffiliateRepositoryPort;
import com.meetbowl.domain.organization.Department;
import com.meetbowl.domain.organization.DepartmentRepositoryPort;
import com.meetbowl.domain.organization.Position;
import com.meetbowl.domain.organization.PositionRepositoryPort;
import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.domain.organization.Team;
import com.meetbowl.domain.organization.TeamRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserSearchReindexEventPublisherPort;
import com.meetbowl.domain.user.UserSearchReindexRequestedEvent;
import com.meetbowl.domain.user.UserStatus;

class MyProfileUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID AFFILIATE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID DEPARTMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID POSITION_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final Instant NOW = Instant.parse("2026-06-12T00:00:00Z");

    private FakeUserRepository userRepository;
    private FakeUserSearchReindexEventPublisherPort userSearchReindexEventPublisherPort;
    private UserSearchReindexRequestDispatcher userSearchReindexRequestDispatcher;
    private MyProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        FakeAffiliateRepository affiliateRepository = new FakeAffiliateRepository();
        FakeDepartmentRepository departmentRepository = new FakeDepartmentRepository();
        FakeTeamRepository teamRepository = new FakeTeamRepository();
        FakePositionRepository positionRepository = new FakePositionRepository();
        userSearchReindexEventPublisherPort = new FakeUserSearchReindexEventPublisherPort();
        userSearchReindexRequestDispatcher =
                new UserSearchReindexRequestDispatcher(userSearchReindexEventPublisherPort);

        affiliateRepository.save(
                new Affiliate(
                        AFFILIATE_ID, "Affiliate", "AFF", ReferenceStatus.ACTIVE, 1, NOW, NOW));
        departmentRepository.save(
                new Department(
                        DEPARTMENT_ID,
                        AFFILIATE_ID,
                        null,
                        "Department",
                        "DEP",
                        ReferenceStatus.ACTIVE,
                        1,
                        NOW,
                        NOW));
        teamRepository.save(
                new Team(
                        TEAM_ID,
                        DEPARTMENT_ID,
                        "Team",
                        "TEAM",
                        ReferenceStatus.ACTIVE,
                        1,
                        NOW,
                        NOW));
        positionRepository.save(
                new Position(POSITION_ID, "Position", "POS", ReferenceStatus.ACTIVE, 1, NOW, NOW));

        userRepository.save(createUser(USER_ID, "user01", "user01@example.com", UserRole.USER));
        useCase =
                new MyProfileUseCase(
                        userRepository,
                        affiliateRepository,
                        departmentRepository,
                        teamRepository,
                        positionRepository,
                        userSearchReindexRequestDispatcher);
    }

    @Test
    void getProfileSuccessIncludesOrganizationNames() {
        MyProfileResult result = useCase.get(USER_ID);

        assertEquals(USER_ID, result.userId());
        assertEquals("user01", result.loginId());
        assertEquals("USER", result.role());
        assertEquals("ACTIVE", result.status());
        assertEquals("Affiliate", result.affiliate());
        assertEquals("Department", result.department());
        assertEquals("Team", result.team());
        assertEquals("Position", result.position());
    }

    @Test
    void updateProfileSuccessChangesOnlyNameAndEmail() {
        MyProfileResult result =
                useCase.update(
                        new UpdateMyProfileCommand(USER_ID, "Updated User", "updated@example.com"));
        User saved = userRepository.findById(USER_ID).orElseThrow();

        assertEquals("Updated User", result.name());
        assertEquals("updated@example.com", result.email());
        assertEquals("user01", saved.loginId());
        assertEquals(UserRole.USER, saved.role());
        assertEquals(UserStatus.ACTIVE, saved.status());
        assertEquals(AFFILIATE_ID, saved.affiliateId());
        assertEquals(DEPARTMENT_ID, saved.departmentId());
        assertEquals(TEAM_ID, saved.teamId());
        assertEquals(POSITION_ID, saved.positionId());
        assertEquals(Instant.parse("2026-06-01T00:00:00Z"), saved.activeFrom());
        assertEquals(Instant.parse("2026-12-31T23:59:59Z"), saved.activeUntil());
        assertEquals(USER_ID, userSearchReindexEventPublisherPort.lastEvent.userIds().get(0));
        assertEquals("MY_PROFILE_UPDATED", userSearchReindexEventPublisherPort.lastEvent.reason());
    }

    @Test
    void updateProfileFailsWhenEmailAlreadyExists() {
        userRepository.save(
                createUser(OTHER_USER_ID, "user02", "user02@example.com", UserRole.USER));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.update(
                                        new UpdateMyProfileCommand(
                                                USER_ID, "User One", "user02@example.com")));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    private User createUser(UUID id, String loginId, String email, UserRole role) {
        return User.of(
                id,
                loginId,
                "hash",
                "User One",
                email,
                role,
                UserStatus.ACTIVE,
                AFFILIATE_ID,
                DEPARTMENT_ID,
                POSITION_ID,
                TEAM_ID,
                false,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z"),
                NOW,
                NOW);
    }

    private static final class FakeUserRepository implements UserRepositoryPort {
        private final Map<UUID, User> users = new ConcurrentHashMap<>();

        @Override
        public User save(User user) {
            users.put(user.id(), user);
            return user;
        }

        @Override
        public Optional<User> findById(UUID userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public List<User> findAll() {
            return List.copyOf(users.values());
        }

        @Override
        public List<User> findAllForExcelExportByRoles(java.util.Set<UserRole> roles) {
            return users.values().stream().filter(user -> roles.contains(user.role())).toList();
        }

        @Override
        public Optional<User> findByLoginId(String loginId) {
            return users.values().stream()
                    .filter(user -> user.loginId().equals(loginId))
                    .findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return users.values().stream().filter(user -> user.email().equals(email)).findFirst();
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
            return new Paged<>(List.copyOf(users.values()), users.size());
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
            return new Paged<>(List.copyOf(users.values()), users.size());
        }

        @Override
        public List<User> findAllByAffiliateId(UUID affiliateId) {
            return users.values().stream()
                    .filter(user -> affiliateId.equals(user.affiliateId()))
                    .toList();
        }

        @Override
        public List<User> findAllByDepartmentId(UUID departmentId) {
            return users.values().stream()
                    .filter(user -> departmentId.equals(user.departmentId()))
                    .toList();
        }

        @Override
        public List<User> findAllByTeamId(UUID teamId) {
            return users.values().stream().filter(user -> teamId.equals(user.teamId())).toList();
        }

        @Override
        public List<User> findAllByPositionId(UUID positionId) {
            return users.values().stream()
                    .filter(user -> positionId.equals(user.positionId()))
                    .toList();
        }
    }

    private static final class FakeUserSearchReindexEventPublisherPort
            implements UserSearchReindexEventPublisherPort {
        private UserSearchReindexRequestedEvent lastEvent;

        @Override
        public void publish(UserSearchReindexRequestedEvent event) {
            lastEvent = event;
        }
    }

    private static final class FakeAffiliateRepository implements AffiliateRepositoryPort {
        private final Map<UUID, Affiliate> affiliates = new ConcurrentHashMap<>();

        @Override
        public Affiliate save(Affiliate affiliate) {
            affiliates.put(affiliate.id(), affiliate);
            return affiliate;
        }

        @Override
        public Optional<Affiliate> findById(UUID affiliateId) {
            return Optional.ofNullable(affiliates.get(affiliateId));
        }

        @Override
        public List<Affiliate> findAllByIds(Collection<UUID> affiliateIds) {
            return affiliateIds.stream().map(affiliates::get).toList();
        }

        @Override
        public List<Affiliate> findAll() {
            return List.copyOf(affiliates.values());
        }

        @Override
        public List<Affiliate> findAllForExcelExport() {
            return findAll();
        }

        @Override
        public boolean existsByName(String name) {
            return affiliates.values().stream()
                    .anyMatch(affiliate -> affiliate.name().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsByCode(String code) {
            return affiliates.values().stream()
                    .anyMatch(affiliate -> affiliate.code().equalsIgnoreCase(code));
        }

        @Override
        public boolean existsByNameAndIdNot(String name, UUID affiliateId) {
            return affiliates.values().stream()
                    .anyMatch(
                            affiliate ->
                                    !affiliate.id().equals(affiliateId)
                                            && affiliate.name().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsByCodeAndIdNot(String code, UUID affiliateId) {
            return affiliates.values().stream()
                    .anyMatch(
                            affiliate ->
                                    !affiliate.id().equals(affiliateId)
                                            && affiliate.code().equalsIgnoreCase(code));
        }
    }

    private static final class FakeDepartmentRepository implements DepartmentRepositoryPort {
        private final Map<UUID, Department> departments = new ConcurrentHashMap<>();

        @Override
        public Department save(Department department) {
            departments.put(department.id(), department);
            return department;
        }

        @Override
        public Optional<Department> findById(UUID departmentId) {
            return Optional.ofNullable(departments.get(departmentId));
        }

        @Override
        public List<Department> findAllByIds(Collection<UUID> departmentIds) {
            return departmentIds.stream().map(departments::get).toList();
        }

        @Override
        public List<Department> findAll() {
            return List.copyOf(departments.values());
        }

        @Override
        public List<Department> findAllForExcelExport() {
            return findAll();
        }

        @Override
        public boolean existsByAffiliateIdAndName(UUID affiliateId, String name) {
            return departments.values().stream()
                    .anyMatch(
                            department ->
                                    department.affiliateId().equals(affiliateId)
                                            && department.name().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsByAffiliateIdAndNameAndIdNot(
                UUID affiliateId, String name, UUID departmentId) {
            return departments.values().stream()
                    .anyMatch(
                            department ->
                                    !department.id().equals(departmentId)
                                            && department.affiliateId().equals(affiliateId)
                                            && department.name().equalsIgnoreCase(name));
        }
    }

    private static final class FakeTeamRepository implements TeamRepositoryPort {
        private final Map<UUID, Team> teams = new ConcurrentHashMap<>();

        @Override
        public Team save(Team team) {
            teams.put(team.id(), team);
            return team;
        }

        @Override
        public Optional<Team> findById(UUID teamId) {
            return Optional.ofNullable(teams.get(teamId));
        }

        @Override
        public List<Team> findAllByIds(Collection<UUID> teamIds) {
            return teamIds.stream().map(teams::get).toList();
        }

        @Override
        public List<Team> findAll() {
            return List.copyOf(teams.values());
        }

        @Override
        public List<Team> findAllForExcelExport() {
            return findAll();
        }

        @Override
        public boolean existsByDepartmentIdAndName(UUID departmentId, String name) {
            return teams.values().stream()
                    .anyMatch(
                            team ->
                                    team.departmentId().equals(departmentId)
                                            && team.name().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsByDepartmentIdAndNameAndIdNot(
                UUID departmentId, String name, UUID teamId) {
            return teams.values().stream()
                    .anyMatch(
                            team ->
                                    !team.id().equals(teamId)
                                            && team.departmentId().equals(departmentId)
                                            && team.name().equalsIgnoreCase(name));
        }
    }

    private static final class FakePositionRepository implements PositionRepositoryPort {
        private final Map<UUID, Position> positions = new ConcurrentHashMap<>();

        @Override
        public Position save(Position position) {
            positions.put(position.id(), position);
            return position;
        }

        @Override
        public Optional<Position> findById(UUID positionId) {
            return Optional.ofNullable(positions.get(positionId));
        }

        @Override
        public List<Position> findAllByIds(Collection<UUID> positionIds) {
            return positionIds.stream().map(positions::get).toList();
        }

        @Override
        public List<Position> findAll() {
            return List.copyOf(positions.values());
        }

        @Override
        public List<Position> findAllForExcelExport() {
            return findAll();
        }

        @Override
        public boolean existsByName(String name) {
            return positions.values().stream()
                    .anyMatch(position -> position.name().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsByCode(String code) {
            return positions.values().stream()
                    .anyMatch(position -> position.code().equalsIgnoreCase(code));
        }

        @Override
        public boolean existsByNameAndIdNot(String name, UUID positionId) {
            return positions.values().stream()
                    .anyMatch(
                            position ->
                                    !position.id().equals(positionId)
                                            && position.name().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsByCodeAndIdNot(String code, UUID positionId) {
            return positions.values().stream()
                    .anyMatch(
                            position ->
                                    !position.id().equals(positionId)
                                            && position.code().equalsIgnoreCase(code));
        }
    }
}
